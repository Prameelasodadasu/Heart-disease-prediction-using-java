# ❤️ CardioPredict — Heart Disease Prediction System (Java · Machine Learning · MySQL)

A full-stack web application that estimates a person's **risk of heart disease** from 13 clinical parameters.
The prediction engine is built **from scratch in Java** (no ML libraries) using **K-Nearest Neighbours (KNN)** and
**Logistic Regression**. Patient records are stored in **MySQL**, and the app ships with two interfaces: a simple
**patient page** for taking the risk assessment and a password-protected **admin dashboard** for records and model insights.

> ⚠️ **Disclaimer:** This is an educational project. It is **not** a medical device and must not be used for diagnosis.

---

## 📑 Table of Contents
1. [Screenshots](#-screenshots)
2. [Features](#-features)
3. [Tech Stack](#-tech-stack)
4. [How It Works](#-how-it-works)
5. [Dataset](#-dataset)
6. [Model Performance](#-model-performance)
7. [Getting Started](#-getting-started)
8. [MySQL Setup](#-mysql-setup)
9. [Configuration](#-configuration)
10. [Using the App](#-using-the-app)
11. [REST API](#-rest-api)
12. [Database Schema](#-database-schema)
13. [Project Structure](#-project-structure)
14. [Limitations & Future Work](#-limitations--future-work)
15. [Author](#-author)

---

## 📸 Screenshots

<!--
HOW TO ADD SCREENSHOTS
1. Create a folder in your repo:  docs/screenshots/
2. Save each screenshot with EXACTLY the file name shown in the image links below.
3. Upload them to GitHub (Add file → Upload files, inside docs/screenshots).
The images will then appear automatically. Delete this comment when finished.
-->

### Patient interface

| Empty form (top) | Empty form (bottom) |
|:---:|:---:|
| ![Patient form - top](docs/screenshots/01-patient-form.png) | ![Patient form - bottom](docs/screenshots/01-patient-form2.png) |

| Filled with sample data (top) | Filled with sample data (bottom) |
|:---:|:---:|
| ![Filled form - top](docs/screenshots/02-patient-form-filled.png) | ![Filled form - bottom](docs/screenshots/02-patient-form-filled2.png) |

| Low risk result | Moderate risk result | High risk result |
|:---:|:---:|:---:|
| ![Low risk](/docs/screenshots/03-result-low.png) | ![Moderate risk](docs/screenshots/04-result-moderate.png) | ![High risk](docs/screenshots/05-result-high.png) |

### Admin interface

| Admin login | Dashboard |
|:---:|:---:|
| ![Admin login](docs/screenshots/06-admin-login.png) | ![Dashboard](docs/screenshots/07-admin-dashboard.png) |

| Patient detail view | Model insights |
|:---:|:---:|
| ![Patient detail](docs/screenshots/08-admin-patient-detail.png) | ![Model insights](docs/screenshots/09-admin-model-insights.png) |

### Backend

| Console on startup | Data stored in MySQL |
|:---:|:---:|
| ![Console](docs/screenshots/10-console-startup.png) | ![MySQL table](docs/screenshots/11-mysql-table.png) |

---

## ✨ Features

**Patient interface** (`/`)
- Clean step-by-step form grouped into *About you*, *Vitals & blood tests* and *Heart tests & symptoms*
- Placeholders and helpful hints; nothing is pre-selected until the user chooses (or clicks **Fill sample data**)
- Live progress bar and input validation (client-side and server-side)
- Clear **Low / Moderate / High** result with simple advice and an emergency note
- *Check another person* and *Print result* buttons

**Admin interface** (`/admin`, password protected)
- Dashboard with total assessments, patients added today, average age and high-risk share
- Risk-mix donut chart and a "needs attention" list of recent high-risk patients
- Searchable, filterable patient table with a detailed record view (including both model scores)
- Delete records and **export all data to CSV**
- **Model Insights**: accuracy, precision, recall, F1, confusion matrices, selected features and correlations

**Backend**
- KNN and Logistic Regression implemented from scratch
- Data cleaning, feature selection, normalisation and cross-validated tuning
- MySQL persistence through JDBC, with automatic table creation
- Automatic fallback to in-memory storage when MySQL is not available
- Lightweight REST API served by Java's built-in HTTP server (no frameworks)

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 8+ |
| Machine learning | Custom KNN + Logistic Regression (pure Java) |
| Database | MySQL 8 (via MySQL Connector/J, JDBC) |
| Web server | `com.sun.net.httpserver` (built into the JDK) |
| Frontend | HTML, CSS, vanilla JavaScript |
| Build | Maven (optional) or plain `javac` / `run.bat` |

---

## 🔍 How It Works

```
 Patient form ──▶ REST API ──▶ Validation ──▶ Feature selection ──▶ Normalisation
                                                                         │
                                         ┌───────────────────────────────┴──────────────┐
                                         ▼                                              ▼
                                   KNN probability                       Logistic Regression probability
                                         └───────────────────────┬──────────────────────┘
                                                                 ▼
                                                  Average → Risk level (Low / Moderate / High)
                                                                 ▼
                                             Result shown to patient + record saved to MySQL
```

### Machine-learning pipeline

| Step | What happens | Class |
|---|---|---|
| 1. Data cleaning | Removes duplicates and unlabeled rows; fills missing values with the column median | `Dataset` |
| 2. Train/test split | 80% training, 20% testing, shuffled with a fixed seed for reproducibility | `ModelService` |
| 3. Feature selection | Keeps features whose absolute Pearson correlation with the target is ≥ 0.10 (minimum 5 features) | `FeatureSelector` |
| 4. Normalisation | Z-score scaling, fitted on training data only to avoid leakage | `Scaler` |
| 5. KNN | Euclidean distance with majority vote; **K is tuned with 5-fold cross-validation** (odd K from 3 to 21) | `KNN` |
| 6. Logistic Regression | Batch gradient descent with L2 regularisation | `LogisticRegression` |
| 7. Evaluation | Accuracy, precision, recall, F1 and confusion matrix on unseen test data | `Metrics` |
| 8. Prediction | Final score = average of both models' probabilities | `ModelService` |

**Risk levels:** score below 35% → **Low**, 35% to 60% → **Moderate**, 60% and above → **High**.

---

## 📊 Dataset

The model expects 13 input features and one target (`target`: 1 = heart disease, 0 = no heart disease),
following the structure of the UCI Cleveland Heart Disease dataset.

| Feature | Description | Values |
|---|---|---|
| `age` | Age in years | 1–120 |
| `sex` | Sex | 1 = male, 0 = female |
| `cp` | Chest pain type | 0 typical angina, 1 atypical angina, 2 non-anginal, 3 asymptomatic |
| `trestbps` | Resting blood pressure (mm Hg) | 50–300 |
| `chol` | Serum cholesterol (mg/dl) | 50–700 |
| `fbs` | Fasting blood sugar > 120 mg/dl | 1 = yes, 0 = no |
| `restecg` | Resting ECG result | 0 normal, 1 ST-T abnormality, 2 LV hypertrophy |
| `thalach` | Maximum heart rate achieved | 40–250 |
| `exang` | Exercise-induced angina | 1 = yes, 0 = no |
| `oldpeak` | ST depression induced by exercise | 0–10 |
| `slope` | Slope of the peak exercise ST segment | 0 upsloping, 1 flat, 2 downsloping |
| `ca` | Major vessels coloured by fluoroscopy | 0–3 |
| `thal` | Thalassemia test | 1 normal, 2 fixed defect, 3 reversible defect |

> **Note on the bundled data:** `src/main/resources/data/heart.csv` is a **synthetic sample dataset** (900 rows) generated by
> `tools/generate_dataset.py` so the project runs out of the box. For real-world results, replace it with the
> [UCI Heart Disease dataset](https://archive.ics.uci.edu/dataset/45/heart+disease) using the same column order
> (or point the `DATA_FILE` environment variable to your own CSV). If your file encodes `thal` differently, adjust the dropdown
> values in `web/index.html`.

---

## 📈 Model Performance

Results on the bundled sample dataset (720 training rows, 180 unseen test rows):

| Model | Accuracy |
|---|---|
| K-Nearest Neighbours (K = 17, chosen by cross-validation) | ≈ 81% |
| Logistic Regression | ≈ 85% |

Precision, recall, F1-score and confusion matrices are available on the **Model Insights** page of the admin dashboard.
Figures will differ when you train on a different dataset.

---

## 🚀 Getting Started

### Prerequisites
- **JDK 8 or newer** (check with `javac -version`)
- **MySQL** is optional — without it, records are kept in memory
- **Maven** is optional

### Option A — Windows (easiest)
1. Download or clone this repository.
2. Double-click **`run.bat`**.
3. Open **http://localhost:8080** (patient page) or **http://localhost:8080/admin** (admin page).

`run.bat` compiles the code, starts the app and, if a MySQL driver jar is present in the folder, connects to MySQL.

### Option B — Command line (any OS)
```bash
# compile
mkdir out
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
cp -r src/main/resources/* out/

# run (Linux/macOS use ':' ; Windows uses ';' as the classpath separator)
java -cp out com.heartpredict.Main
```

### Option C — Maven
```bash
mvn compile exec:java
```

---

## 🗄️ MySQL Setup

1. **Install MySQL Server** and remember your root password. Make sure the service is running.
2. **Download the Connector/J driver** (JDBC driver for MySQL) as a `.jar` file, for example
   `mysql-connector-j-8.4.0.jar`, and place it in the project's root folder (next to `pom.xml`).
   *(Not needed with Maven, which downloads it automatically.)*
3. **Run `run.bat`** and enter your MySQL password when asked, or set the variables manually:
   ```bat
   set DB_USER=root
   set DB_PASS=your_password
   java -cp "out;mysql-connector-j-8.4.0.jar" com.heartpredict.Main
   ```
4. The console shows `[DB] Connected to MySQL.` The database `heart_db` and table `patients` are created automatically.
5. Verify the data:
   ```sql
   USE heart_db;
   SELECT * FROM patients;
   ```

If MySQL cannot be reached, the app keeps working with **in-memory storage** (data is lost when the app stops).

---

## ⚙️ Configuration

All settings are optional environment variables.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Web server port |
| `ADMIN_PASS` | `admin123` | Admin dashboard password — **change this** |
| `DB_USER` | `root` | MySQL username |
| `DB_PASS` | `root` | MySQL password |
| `DB_URL` | `jdbc:mysql://localhost:3306/heart_db?...` | Custom JDBC URL (different host/port) |
| `DATA_FILE` | bundled CSV | Path to your own training CSV |

---

## 🖥️ Using the App

**As a patient**
1. Open `http://localhost:8080/`.
2. Fill in the questions (or click **Fill sample data** to try it).
3. Click **See my result** to get a Low, Moderate or High risk result with simple advice.

**As an admin**
1. Open `http://localhost:8080/admin` and sign in.
2. Review the dashboard, search and filter patients, open a record for details, delete records or export a CSV.
3. Open **Model insights** to view accuracy, confusion matrices and feature selection.

---

## 🔌 REST API

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/predict` | Public | Form-encoded features + `patient_name`; returns the risk level and saves the record |
| `POST` | `/api/admin/login` | Public | Admin login (`password`) — sets a session cookie |
| `GET` | `/api/admin/logout` | Admin | Ends the session |
| `GET` | `/api/patients` | Admin | Lists saved patients with their scores |
| `DELETE` | `/api/patients?id=N` | Admin | Deletes a record |
| `GET` | `/api/model` | Admin | Metrics, selected features and correlations |

---

## 🧱 Database Schema

Created automatically on first run (`database/schema.sql` is included for reference).

```sql
CREATE TABLE patients (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_name VARCHAR(120) NOT NULL,
  age DOUBLE, sex DOUBLE, cp DOUBLE, trestbps DOUBLE, chol DOUBLE, fbs DOUBLE,
  restecg DOUBLE, thalach DOUBLE, exang DOUBLE, oldpeak DOUBLE, slope DOUBLE,
  ca DOUBLE, thal DOUBLE,
  knn_prob DOUBLE, lr_prob DOUBLE, risk_score DOUBLE, risk_level VARCHAR(20),
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

SQL operations used: `CREATE TABLE IF NOT EXISTS` (startup), `INSERT` (new assessment), `SELECT` (admin records), `DELETE` (remove record).

---

## 📁 Project Structure

```
heart-disease-prediction/
├── pom.xml                          # Maven config (MySQL driver, exec plugin)
├── run.bat                          # One-click build & run for Windows
├── README.md
├── database/
│   └── schema.sql                   # Reference SQL schema
├── tools/
│   └── generate_dataset.py          # Generates the synthetic sample dataset
├── docs/
│   └── screenshots/                 # README images
└── src/main/
    ├── java/com/heartpredict/
    │   ├── Main.java                # HTTP server, routes, admin auth, validation
    │   ├── ml/
    │   │   ├── Dataset.java         # CSV loading, cleaning, imputation
    │   │   ├── FeatureSelector.java # Correlation-based feature selection
    │   │   ├── Scaler.java          # Z-score normalisation
    │   │   ├── KNN.java             # K-Nearest Neighbours
    │   │   ├── LogisticRegression.java
    │   │   ├── Metrics.java         # Accuracy / precision / recall / F1
    │   │   └── ModelService.java    # Full training & prediction pipeline
    │   └── db/
    │       └── PatientRepository.java  # MySQL access + in-memory fallback
    └── resources/
        ├── data/heart.csv           # Sample dataset
        └── web/
            ├── index.html           # Patient interface
            └── admin.html           # Admin dashboard
```

---

## 🔭 Limitations & Future Work

- The bundled dataset is synthetic; train on real clinical data for meaningful results
- Single shared admin password — add user accounts and hashed credentials for production use
- Add more models (Random Forest, SVM) and compare them on the Model Insights page
- Add charts for risk trends over time and patient follow-up tracking
- Dockerise the app together with MySQL (`docker-compose`)
- Add unit tests for the ML classes

---

## 👤 Author

**Your Name** — [GitHub](https://github.com/your-username) · [LinkedIn](https://www.linkedin.com/in/your-profile)

*If you found this project useful, consider giving it a ⭐.*
