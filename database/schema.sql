-- Optional: the app creates this automatically on startup.
CREATE DATABASE IF NOT EXISTS heart_db;
USE heart_db;
CREATE TABLE IF NOT EXISTS patients (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_name VARCHAR(120) NOT NULL,
  age DOUBLE, sex DOUBLE, cp DOUBLE, trestbps DOUBLE, chol DOUBLE, fbs DOUBLE,
  restecg DOUBLE, thalach DOUBLE, exang DOUBLE, oldpeak DOUBLE, slope DOUBLE,
  ca DOUBLE, thal DOUBLE,
  knn_prob DOUBLE, lr_prob DOUBLE, risk_score DOUBLE, risk_level VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
