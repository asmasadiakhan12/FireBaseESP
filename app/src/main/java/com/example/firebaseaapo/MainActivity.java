

package com.example.firebaseaapo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseTest";
    private static final int REQUEST_PERMISSIONS = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView bpmTextView, spo2TextView, tempTextView, resultTextView, statusTextView;
    private Interpreter tflite;
    private DatabaseReference databaseReference;
    private String alertEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("healthData");

        // Initialize UI components
        bpmTextView = findViewById(R.id.bpmTextView);
        spo2TextView = findViewById(R.id.spo2TextView);
        tempTextView = findViewById(R.id.tempTextView);
        resultTextView = findViewById(R.id.resultTextView);
        statusTextView = findViewById(R.id.statusTextView);
        Button connectButton = findViewById(R.id.connectButton);
        Button saveEmailButton = findViewById(R.id.saveEmailButton);
        TextView emailEditText = findViewById(R.id.emailEditText);

        // Save the email address for alerts
        saveEmailButton.setOnClickListener(v -> {
            alertEmail = emailEditText.getText().toString();
            Toast.makeText(this, "Email saved: " + alertEmail, Toast.LENGTH_SHORT).show();
        });

        // Load the TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile());
            Log.d(TAG, "TFLite model loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading TFLite model.", e);
        }

        // Initialize the Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Connect to Bluetooth device
        connectButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkPermissionsAndShowPairedDevices();
            } else {
                showPairedDevicesDialog();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPermissionsAndShowPairedDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
            }, REQUEST_PERMISSIONS);
        } else {
            showPairedDevicesDialog();
        }
    }

    private void showPairedDevicesDialog() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> deviceNames = new ArrayList<>();
        ArrayList<BluetoothDevice> devices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName());
            devices.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Device");
        builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
            BluetoothDevice selectedDevice = devices.get(which);
            connectToDevice(selectedDevice);
        });

        builder.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            Log.d(TAG, "Attempting to connect to device: " + device.getName());
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            Log.d(TAG, "Connected to " + device.getName());
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            startListeningForData();
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect to device: " + device.getName(), e);
            Toast.makeText(this, "Failed to connect to " + device.getName(), Toast.LENGTH_SHORT).show();
            try {
                bluetoothSocket.close();
            } catch (IOException ex) {
                Log.e(TAG, "Failed to close socket after failed connection", ex);
            }
        }
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    processData(data.trim());
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from input stream", e);
                    break;
                }
            }
        }).start();
    }

    private void processData(String data) {
        try {
            String[] values = data.split(",");
            if (values.length == 3) {
                float bpm = Float.parseFloat(values[0]);
                float spo2 = Float.parseFloat(values[1]);
                float temp = Float.parseFloat(values[2]);

                // Prepare input data for the model
                float[][] input = new float[1][3];
                input[0][0] = bpm;
                input[0][1] = spo2;
                input[0][2] = temp;

                // Prepare output buffer
                float[][] output = new float[1][1];

                // Run inference using the model
                tflite.run(input, output);

                // Get the prediction result
                float predictionValue = output[0][0];
                String prediction = predictionValue > 0.5 ? "Positive" : "Negative";

                // Update UI with the data and prediction
                runOnUiThread(() -> {
                    bpmTextView.setText(getString(R.string.bpm_text, bpm));
                    spo2TextView.setText(getString(R.string.spo2_text, spo2));
                    tempTextView.setText(getString(R.string.temperature_text, temp));
                    resultTextView.setText(getString(R.string.prediction_text, prediction));
                });

                // Send data to Firebase
                sendDataToFirebase(bpm, spo2, temp, prediction);

                // If the prediction is "Positive", send an email alert
                if ("Positive".equals(prediction)) {
                    sendEmailAlert(bpm, spo2, temp);
                }
            } else {
                Log.e(TAG, "Invalid data format. Expected 3 values, got " + values.length);
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Invalid data format.");
                    Toast.makeText(this, "Invalid data format.", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse data: " + e.getMessage(), e);
            runOnUiThread(() -> {
                statusTextView.setText("Status: Error parsing data.");
                Toast.makeText(this, "Error parsing data.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Send data to Firebase
    private void sendDataToFirebase(float bpm, float spo2, float temp, String prediction) {
        String key = databaseReference.push().getKey();
        if (key == null) {
            Log.e(TAG, "Failed to get a new key for Firebase.");
            return;
        }

        HealthData healthData = new HealthData(bpm, spo2, temp, prediction, System.currentTimeMillis());
        databaseReference.child(key).setValue(healthData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Data sent to Firebase successfully.");
                    } else {
                        Log.e(TAG, "Failed to send data to Firebase.", task.getException());
                    }
                });
    }

    // Send email alert
    private void sendEmailAlert(float bpm, float spo2, float temp) {
        if (alertEmail == null || alertEmail.isEmpty()) {
            Log.e(TAG, "No alert email set. Cannot send email.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication("valobaby30@gmail.com", "meyy xywb syvv tsrm");
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("valobaby30@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(alertEmail));
            message.setSubject("ESP Monitoring Alert: Positive Result");
            message.setText("Alert! A positive result was detected.\n\n" +
                    "BPM: " + bpm + "\n" +
                    "SpO2: " + spo2 + "\n" +
                    "Temperature: " + temp);

            Transport.send(message);
            Log.d(TAG, "Alert email sent to: " + alertEmail);

        } catch (MessagingException e) {
            Log.e(TAG, "Failed to send email", e);
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
            if (tflite != null) tflite.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPairedDevicesDialog();
            } else {
                Log.e(TAG, "Permissions denied. Cannot proceed with Bluetooth connection.");
                Toast.makeText(this, "Permissions denied. Please grant Bluetooth permissions.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

// HealthData class to represent data sent to Firebase
class HealthData {
    public float bpm;
    public float spo2;
    public float temperature;
    public String prediction;
    public long timestamp;

    public HealthData() {
        // Default constructor required for calls to DataSnapshot.getValue(HealthData.class)
    }

    public HealthData(float bpm, float spo2, float temperature, String prediction, long timestamp) {
        this.bpm = bpm;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.prediction = prediction;
        this.timestamp = timestamp;
    }
}
