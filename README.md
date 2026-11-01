# Pharmacy Inventory & Billing Management System

A desktop application built in **Java Swing** with **MySQL database persistence** to manage pharmacy stock, monitor expiry and low-stock alerts, generate customer invoices, and maintain sales transaction logs.

---

## 🚀 Features

- **Digital Stock Register**: View full inventory details including Batch Number, Manufacturing Date, Expiration Date, Supplier, and Price.
- **Stock Management**: Add new medicine batches or increment stock quantities for existing records with automatic database synchronization.
- **Smart Alerts**:
  - **Low Stock Warning**: Highlights items whose inventory falls below the threshold ($\le$ 15 units).
  - **Expiry Warning**: Alerts for medicines nearing expiration within 60 days or expired items.
- **Billing & POS Checkout**:
  - Select medicines from real-time stock, specify purchase quantities, and calculate subtotals.
  - Automatically prevents selling expired medicines or quantities exceeding current stock.
  - Generates itemized invoices upon checkout and decrements stock in MySQL atomically.
- **Transaction History**: Keeps a complete record of all customer orders, purchased items, and total billing amounts.

---

## 🛠️ Tech Stack & Prerequisites

- **Language**: Java 17 or higher
- **UI Framework**: Java Swing
- **Database**: MySQL Server 8.0+
- **Driver**: MySQL Connector/J 8.3.0 (`mysql-connector-j-8.3.0.jar`)

---

## 🗄️ Database Setup

1. Make sure your MySQL Server is running on port `3306`.
2. Execute the included SQL setup script either in **MySQL Workbench** or via terminal:
   ```bash
   mysql -u root -p < setup_pharmacy_db.sql
   ```
   This creates the `pharmacy_db` schema along with the `medicines`, `transactions`, and `transaction_items` tables, and pre-populates sample medicine batches.

---

## 💻 Running the Application

### Option 1: Quick Launch (Windows)
Double-click **`run.bat`** or run:
```cmd
run.bat
```

### Option 2: Manual Compile & Run
In your terminal, run:

```bash
# 1. Compile Java source files with the MySQL JDBC driver
javac --release 17 -cp ".;mysql-connector-j-8.3.0.jar" project.java DatabaseHelper.java main.java

# 2. Run the application
java -cp ".;mysql-connector-j-8.3.0.jar" PharmacyManagementSystem
```

---

## ⚙️ Configuration (Optional)

By default, the application connects using:
- **Host**: `localhost:3306`
- **User**: `root`
- **Database**: `pharmacy_db`

You can override these using environment variables:
- `MYSQL_HOST`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

---

## 📂 Project Structure

```text
├── project.java                # Main application UI and logic (PharmacyManagementSystem)
├── DatabaseHelper.java         # JDBC MySQL connection & data access layer
├── main.java                   # Compatibility application entry point
├── setup_pharmacy_db.sql       # Database schema creation and sample seed script
├── mysql-connector-j-8.3.0.jar # MySQL JDBC driver
├── run.bat                     # Windows one-click build and execution script
├── README.md                   # Project documentation
└── .gitignore                  # Git ignore rules for class files
```

