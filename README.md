Overview
This project integrates real-time health monitoring with a voice assistant for early detection of respiratory diseases. It utilizes ESP-based sensors to measure SpO₂, BPM, and body temperature, combined with an Android Studio mobile app and machine learning models for prediction. Google Firebase is used for cloud storage, and email alerts notify caregivers for timely medical intervention.

Features
✅ Real-time health data monitoring (SpO₂, BPM, temperature)
✅ Voice assistant integration for hands-free operation
✅ Machine learning-based early respiratory disease prediction
✅ Google Firebase for secure data storage
✅ Automated email alerts to caregivers for emergency intervention
✅ ESP module integration for sensor connectivity

Hardware & Technologies Used
ESP32 / ESP8266 (for sensor data acquisition)

SpO₂, Heart Rate, and Temperature Sensors

Android Studio (for mobile app development)

Google Firebase (for cloud data storage)

Machine Learning Models (for respiratory disease prediction)

Google Text-to-Speech (TTS) (for voice assistant integration)

Email API (SMTP / Firebase Cloud Functions) (for caregiver notifications)

System Architecture
Sensor Data Collection

ESP module collects SpO₂, BPM, and temperature in real-time

Data transmitted to Android mobile app via Bluetooth/WiFi

Cloud Storage & Processing

Data is sent to Google Firebase for storage

Machine learning models analyze the data for respiratory disease detection

Voice Assistant & Alerts

Voice assistant provides spoken health updates

Email alerts are sent to caregivers when critical conditions are detected

Installation & Setup
Prerequisites
Android Studio

Firebase account

ESP32 / ESP8266 module

Python (for ML model training)
