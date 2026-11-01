-- ========================================================
-- PHARMACY INVENTORY & BILLING SYSTEM - DATABASE SETUP
-- ========================================================

CREATE DATABASE IF NOT EXISTS pharmacy_db;
USE pharmacy_db;

-- 1. Create Medicines Table
CREATE TABLE IF NOT EXISTS medicines (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    mfg_date DATE NOT NULL,
    exp_date DATE NOT NULL,
    supplier VARCHAR(100) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL
);

-- 2. Create Transactions / Bills Table
CREATE TABLE IF NOT EXISTS transactions (
    bill_id VARCHAR(20) PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    bill_date DATE NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL
);

-- 3. Create Transaction Items Table
CREATE TABLE IF NOT EXISTS transaction_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id VARCHAR(20) NOT NULL,
    medicine_id VARCHAR(10) NOT NULL,
    quantity_sold INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (bill_id) REFERENCES transactions(bill_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

-- 4. Insert / Update All 10 Sample Medicines
INSERT INTO medicines (id, name, batch_number, quantity, mfg_date, exp_date, supplier, unit_price)
VALUES
('M001', 'Amoxicillin', 'B101', 8, CURDATE() - INTERVAL 6 MONTH, CURDATE() + INTERVAL 25 DAY, 'PharmaCorp', 12.50),
('M002', 'Paracetamol', 'B102', 50, CURDATE() - INTERVAL 2 MONTH, CURDATE() + INTERVAL 12 MONTH, 'HealthMeds', 3.00),
('M003', 'Cough Syrup(S)', 'B103', 5, CURDATE() - INTERVAL 1 YEAR, CURDATE() - INTERVAL 5 DAY, 'SyrupCo', 8.00),
('M004', 'Ibuprofen', 'B104', 20, CURDATE() - INTERVAL 3 MONTH, CURDATE() + INTERVAL 8 MONTH, 'PainRelief Inc.', 5.50),
('M005', 'Antacid', 'B105', 15, CURDATE() - INTERVAL 1 MONTH, CURDATE() + INTERVAL 6 MONTH, 'DigestiveCare', 4.00),
('M006', 'Vitamin C', 'B106', 40, CURDATE() - INTERVAL 6 MONTH, CURDATE() + INTERVAL 18 MONTH, 'Vitality', 2.50),
('M007', 'Amoxicillin', 'B107', 10, CURDATE() + INTERVAL 2 MONTH, CURDATE() + INTERVAL 10 MONTH, 'PharmaCorp', 12.50),
('M008', 'Cough Syrup(A)', 'B108', 3, CURDATE() - INTERVAL 20 DAY, CURDATE() + INTERVAL 10 DAY, 'SyrupCo', 8.00),
('M009', 'Paracetamol', 'B109', 100, CURDATE() - INTERVAL 4 MONTH, CURDATE() + INTERVAL 15 MONTH, 'HealthMeds', 3.00),
('M010', 'Pain Relief Gel', 'B110', 25, CURDATE() - INTERVAL 2 MONTH, CURDATE() + INTERVAL 5 MONTH, 'TopicalCare', 7.25)
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    batch_number = VALUES(batch_number),
    quantity = VALUES(quantity),
    mfg_date = VALUES(mfg_date),
    exp_date = VALUES(exp_date),
    supplier = VALUES(supplier),
    unit_price = VALUES(unit_price);

-- 5. Insert Sample Historical Transaction
INSERT INTO transactions (bill_id, customer_name, bill_date, total_amount)
VALUES ('INV-1000', 'John Doe', CURDATE() - INTERVAL 1 DAY, 25.00)
ON DUPLICATE KEY UPDATE customer_name = VALUES(customer_name);

INSERT INTO transaction_items (bill_id, medicine_id, quantity_sold, subtotal)
SELECT 'INV-1000', 'M001', 2, 25.00
WHERE NOT EXISTS (SELECT 1 FROM transaction_items WHERE bill_id = 'INV-1000' AND medicine_id = 'M001');

SELECT 'DATABASE SETUP COMPLETE!' AS Status;
SELECT * FROM medicines;

