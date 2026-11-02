import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

class PharmacyManagementSystem {

    // --- Interface ---
    interface Billable {
        double calculateTotalValue();
    }

    // --- Abstract Class ---
    abstract static class MedicineCategory {
        public abstract String getCategory();
    }

    // --- Data Model: Medicine Batch (Inheritance + Interface Implementation) ---
    static class MedicineBatch extends MedicineCategory implements Billable {
        private String id;
        private String name;
        private String batchNumber;
        private int quantity;
        private LocalDate manufacturingDate;
        private LocalDate expiryDate;
        private String supplier;
        private double unitPrice;

        public MedicineBatch(String id, String name, String batchNumber, int quantity,
                LocalDate manufacturingDate, LocalDate expiryDate,
                String supplier, double unitPrice) {
            this.id = id;
            this.name = name;
            this.batchNumber = batchNumber;
            this.quantity = quantity;
            this.manufacturingDate = manufacturingDate;
            this.expiryDate = expiryDate;
            this.supplier = supplier;
            this.unitPrice = unitPrice;
        }

        @Override
        public String getCategory() {
            return "Pharmacy Medicine";
        }

        @Override
        public double calculateTotalValue() {
            return quantity * unitPrice;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setUnitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
        }

        public void setExpiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
        }

        public void setSupplier(String supplier) {
            this.supplier = supplier;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public LocalDate getManufacturingDate() {
            return manufacturingDate;
        }

        public String getSupplier() {
            return supplier;
        }

        public double getUnitPrice() {
            return unitPrice;
        }
    }

    // --- Data Model: Sale Item & Bill ---
    static class SaleItem {
        MedicineBatch batch;
        int quantitySold;
        double subtotal;

        public SaleItem(MedicineBatch batch, int quantitySold) {
            this.batch = batch;
            this.quantitySold = quantitySold;
            this.subtotal = quantitySold * batch.getUnitPrice();
        }
    }

    static class TransactionRecord {
        String billId;
        String customerName;
        LocalDate date;
        List<SaleItem> items;
        double totalAmount;

        public TransactionRecord(String billId, String customerName, LocalDate date, List<SaleItem> items,
                double totalAmount) {
            this.billId = billId;
            this.customerName = customerName;
            this.date = date;
            this.items = items;
            this.totalAmount = totalAmount;
        }
    }

    // --- Database Access Layer (MySQL Persistence) ---
    static class DatabaseHelper {
        private static final String HOST = System.getenv("MYSQL_HOST") != null ? System.getenv("MYSQL_HOST") : "localhost:3306";
        private static final String HOST_URL = "jdbc:mysql://" + HOST + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        private static final String DB_URL = "jdbc:mysql://" + HOST + "/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        private static final String USER = System.getenv("MYSQL_USER") != null ? System.getenv("MYSQL_USER") : "root";
        private static final String PASSWORD = System.getenv("MYSQL_PASSWORD") != null ? System.getenv("MYSQL_PASSWORD") : "JerolD@#1705";

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, USER, PASSWORD);
        }

        public static void initializeDatabase() {
            try (Connection conn = DriverManager.getConnection(HOST_URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS pharmacy_db");
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] DB check notice: " + e.getMessage());
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS medicines (" +
                        "id VARCHAR(10) PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "batch_number VARCHAR(50) NOT NULL, " +
                        "quantity INT NOT NULL, " +
                        "mfg_date DATE NOT NULL, " +
                        "exp_date DATE NOT NULL, " +
                        "supplier VARCHAR(100) NOT NULL, " +
                        "unit_price DECIMAL(10, 2) NOT NULL" +
                        ")");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                        "bill_id VARCHAR(20) PRIMARY KEY, " +
                        "customer_name VARCHAR(100) NOT NULL, " +
                        "bill_date DATE NOT NULL, " +
                        "total_amount DECIMAL(10, 2) NOT NULL" +
                        ")");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transaction_items (" +
                        "item_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "bill_id VARCHAR(20) NOT NULL, " +
                        "medicine_id VARCHAR(10) NOT NULL, " +
                        "quantity_sold INT NOT NULL, " +
                        "subtotal DECIMAL(10, 2) NOT NULL, " +
                        "FOREIGN KEY (bill_id) REFERENCES transactions(bill_id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (medicine_id) REFERENCES medicines(id)" +
                        ")");

            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Table check notice: " + e.getMessage());
            }
        }

        public static void seedIfEmpty() {
            String check = "SELECT COUNT(*) FROM medicines";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(check)) {

                if (rs.next() && rs.getInt(1) == 0) {
                    saveOrUpdateMedicine(new MedicineBatch("M001", "Amoxicillin", "B101", 8,
                            LocalDate.now().minusMonths(6), LocalDate.now().plusDays(25), "PharmaCorp", 12.50));
                    saveOrUpdateMedicine(new MedicineBatch("M002", "Paracetamol", "B102", 50,
                            LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(12), "HealthMeds", 3.00));
                    saveOrUpdateMedicine(new MedicineBatch("M003", "Cough Syrup(S)", "B103", 5,
                            LocalDate.now().minusYears(1), LocalDate.now().minusDays(5), "SyrupCo", 8.00));
                    saveOrUpdateMedicine(new MedicineBatch("M004", "Ibuprofen", "B104", 20,
                            LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(8), "PainRelief Inc.", 5.50));
                    saveOrUpdateMedicine(new MedicineBatch("M005", "Antacid", "B105", 15,
                            LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), "DigestiveCare", 4.00));
                    saveOrUpdateMedicine(new MedicineBatch("M006", "Vitamin C", "B106", 40,
                            LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18), "Vitality", 2.50));
                    saveOrUpdateMedicine(new MedicineBatch("M007", "Amoxicillin", "B107", 10,
                            LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(10), "PharmaCorp", 12.50));
                    saveOrUpdateMedicine(new MedicineBatch("M008", "Cough Syrup(A)", "B108", 3,
                            LocalDate.now().minusDays(20), LocalDate.now().plusDays(10), "SyrupCo", 8.00));
                    saveOrUpdateMedicine(new MedicineBatch("M009", "Paracetamol", "B109", 100,
                            LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(15), "HealthMeds", 3.00));
                    saveOrUpdateMedicine(new MedicineBatch("M010", "Pain Relief Gel", "B110", 25,
                            LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(5), "TopicalCare", 7.25));
                    System.out.println("[DatabaseHelper] Sample medicine records seeded into MySQL.");
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Seed check notice: " + e.getMessage());
            }
        }

        public static List<MedicineBatch> getAllMedicines() {
            List<MedicineBatch> list = new ArrayList<>();
            String query = "SELECT id, name, batch_number, quantity, mfg_date, exp_date, supplier, unit_price FROM medicines";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    list.add(new MedicineBatch(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("batch_number"),
                            rs.getInt("quantity"),
                            rs.getDate("mfg_date").toLocalDate(),
                            rs.getDate("exp_date").toLocalDate(),
                            rs.getString("supplier"),
                            rs.getDouble("unit_price")
                    ));
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Error loading medicines: " + e.getMessage());
            }
            return list;
        }

        public static boolean saveOrUpdateMedicine(MedicineBatch batch) {
            String query = "INSERT INTO medicines (id, name, batch_number, quantity, mfg_date, exp_date, supplier, unit_price) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), " +
                    "batch_number = VALUES(batch_number), " +
                    "quantity = VALUES(quantity), " +
                    "mfg_date = VALUES(mfg_date), " +
                    "exp_date = VALUES(exp_date), " +
                    "supplier = VALUES(supplier), " +
                    "unit_price = VALUES(unit_price)";

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, batch.getId());
                ps.setString(2, batch.getName());
                ps.setString(3, batch.getBatchNumber());
                ps.setInt(4, batch.getQuantity());
                ps.setDate(5, java.sql.Date.valueOf(batch.getManufacturingDate()));
                ps.setDate(6, java.sql.Date.valueOf(batch.getExpiryDate()));
                ps.setString(7, batch.getSupplier());
                ps.setDouble(8, batch.getUnitPrice());

                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Error saving medicine: " + e.getMessage());
                return false;
            }
        }

        public static int getNextBillNumber() {
            String query = "SELECT bill_id FROM transactions";
            int max = 1000;
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String id = rs.getString(1);
                    if (id != null && id.startsWith("INV-")) {
                        try {
                            int num = Integer.parseInt(id.substring(4).trim());
                            if (num > max) {
                                max = num;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Error checking bill number: " + e.getMessage());
            }
            return max + 1;
        }

        public static boolean recordTransaction(TransactionRecord record) {
            String insertTx = "INSERT INTO transactions (bill_id, customer_name, bill_date, total_amount) VALUES (?, ?, ?, ?)";
            String insertItem = "INSERT INTO transaction_items (bill_id, medicine_id, quantity_sold, subtotal) VALUES (?, ?, ?, ?)";
            String updateStock = "UPDATE medicines SET quantity = quantity - ? WHERE id = ?";

            Connection conn = null;
            try {
                conn = getConnection();
                conn.setAutoCommit(false);

                try (PreparedStatement psTx = conn.prepareStatement(insertTx)) {
                    psTx.setString(1, record.billId);
                    psTx.setString(2, record.customerName);
                    psTx.setDate(3, java.sql.Date.valueOf(record.date));
                    psTx.setDouble(4, record.totalAmount);
                    psTx.executeUpdate();
                }

                try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                     PreparedStatement psStock = conn.prepareStatement(updateStock)) {

                    for (SaleItem item : record.items) {
                        psItem.setString(1, record.billId);
                        psItem.setString(2, item.batch.getId());
                        psItem.setInt(3, item.quantitySold);
                        psItem.setDouble(4, item.subtotal);
                        psItem.executeUpdate();

                        psStock.setInt(1, item.quantitySold);
                        psStock.setString(2, item.batch.getId());
                        psStock.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("[DatabaseHelper] Rollback notice: " + ex.getMessage());
                    }
                }
                System.err.println("[DatabaseHelper] Error recording transaction: " + e.getMessage());
                return false;
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ignored) {}
                }
            }
        }

        public static List<TransactionRecord> getAllTransactions() {
            List<TransactionRecord> list = new ArrayList<>();
            String queryTx = "SELECT bill_id, customer_name, bill_date, total_amount FROM transactions ORDER BY bill_date DESC";
            String queryItems = "SELECT ti.medicine_id, ti.quantity_sold, ti.subtotal, m.name, m.batch_number, m.unit_price, m.mfg_date, m.exp_date, m.supplier, m.quantity " +
                    "FROM transaction_items ti " +
                    "JOIN medicines m ON ti.medicine_id = m.id " +
                    "WHERE ti.bill_id = ?";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rsTx = stmt.executeQuery(queryTx)) {

                while (rsTx.next()) {
                    String billId = rsTx.getString("bill_id");
                    String customerName = rsTx.getString("customer_name");
                    LocalDate date = rsTx.getDate("bill_date").toLocalDate();
                    double totalAmount = rsTx.getDouble("total_amount");

                    List<SaleItem> items = new ArrayList<>();
                    try (PreparedStatement psItems = conn.prepareStatement(queryItems)) {
                        psItems.setString(1, billId);
                        try (ResultSet rsItem = psItems.executeQuery()) {
                            while (rsItem.next()) {
                                MedicineBatch batch = new MedicineBatch(
                                        rsItem.getString("medicine_id"),
                                        rsItem.getString("name"),
                                        rsItem.getString("batch_number"),
                                        rsItem.getInt("quantity"),
                                        rsItem.getDate("mfg_date").toLocalDate(),
                                        rsItem.getDate("exp_date").toLocalDate(),
                                        rsItem.getString("supplier"),
                                        rsItem.getDouble("unit_price")
                                );
                                items.add(new SaleItem(batch, rsItem.getInt("quantity_sold")));
                            }
                        }
                    }
                    list.add(new TransactionRecord(billId, customerName, date, items, totalAmount));
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseHelper] Error loading transactions: " + e.getMessage());
            }
            return list;
        }
    }

    // --- State Storage ---
    // Main in-memory stock database. Each element stores one medicine batch.
    private static final MedicineBatch[] Stock = new MedicineBatch[100];
    private static int stockCount = 0;
    private static final Map<String, MedicineBatch> inventory = new HashMap<>();
    private static final List<TransactionRecord> transactionHistory = new ArrayList<>();
    private static final int LOW_STOCK_THRESHOLD = 15;
    private static final int EXPIRY_WARNING_DAYS = 60;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static int billCounter = 1001;
    private static String pendingCustomerName;
    private static final List<SaleItem> pendingBillItems = new ArrayList<>();

    public static void syncFromDatabase() {
        stockCount = 0;
        inventory.clear();
        transactionHistory.clear();

        List<MedicineBatch> dbMedicines = DatabaseHelper.getAllMedicines();
        for (MedicineBatch b : dbMedicines) {
            if (stockCount < Stock.length) {
                Stock[stockCount++] = b;
                inventory.put(b.getId(), b);
            }
        }

        List<TransactionRecord> dbTransactions = DatabaseHelper.getAllTransactions();
        transactionHistory.addAll(dbTransactions);
        billCounter = DatabaseHelper.getNextBillNumber();

        // Fallback to sample data if database is empty or offline
        if (inventory.isEmpty()) {
            seedSampleData();
        }
    }

    public static void main(String[] args) {
        DatabaseHelper.initializeDatabase();
        DatabaseHelper.seedIfEmpty();
        syncFromDatabase();
        SwingUtilities.invokeLater(() -> new PharmacyFrame().setVisible(true));
    }

    // --- Feature 1: Digital Stock Register ---
    static void viewStockRegister() {
        System.out.println("\n----------------------------- DIGITAL STOCK REGISTER -----------------------------");
        if (inventory.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        System.out.printf("%-6s | %-16s | %-10s | %-6s | %-12s | %-12s | %-10s | %-8s\n",
                "ID", "Name", "Batch No", "Stock", "Mfg Date", "Exp Date", "Supplier", "Price(₹)");
        System.out.println("----------------------------------------------------------------------------------");
        for (int i = 0; i < stockCount; i++) {
            MedicineBatch m = Stock[i];
            System.out.printf("%-6s | %-16s | %-10s | %-6d | %-12s | %-12s | %-10s | ₹%-7.2f\n",
                    m.getId(), m.getName(), m.getBatchNumber(), m.getQuantity(),
                    m.getManufacturingDate().format(DATE_FORMAT),
                    m.getExpiryDate().format(DATE_FORMAT),
                    m.getSupplier(), m.getUnitPrice());
        }
    }

    // --- Feature 2: Add / Update Stock ---
    static void addOrUpdateMedicine(Scanner scanner) {
        System.out.println("\n--- Add / Update Medicine Batch ---");
        try {
            System.out.print("Enter Unique Batch/Stock ID (e.g., M001): ");
            String id = scanner.nextLine().trim();

            if (inventory.containsKey(id)) {
                System.out.print("Item exists. Enter new stock quantity to ADD: ");
                int addQty = Integer.parseInt(scanner.nextLine().trim());
                if (addQty <= 0)
                    throw new IllegalArgumentException("Quantity to add must be positive");
                MedicineBatch existing = inventory.get(id);
                existing.setQuantity(existing.getQuantity() + addQty);
                DatabaseHelper.saveOrUpdateMedicine(existing);
                syncFromDatabase();
                System.out.println("Stock updated! New quantity: " + existing.getQuantity());
                return;
            }

            System.out.print("Medicine Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Batch Number: ");
            String batchNo = scanner.nextLine().trim();
            System.out.print("Initial Quantity: ");
            int qty = Integer.parseInt(scanner.nextLine().trim());
            if (qty <= 0)
                throw new IllegalArgumentException("Initial quantity must be positive");
            System.out.print("Manufacturing Date (YYYY-MM-DD): ");
            LocalDate mfgDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMAT);
            System.out.print("Expiry Date (YYYY-MM-DD): ");
            LocalDate expDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMAT);
            if (mfgDate.isAfter(expDate))
                throw new IllegalArgumentException("Manufacturing date cannot be after expiry date");
            if (expDate.isBefore(LocalDate.now()))
                throw new IllegalArgumentException("New medicine cannot already be expired");
            System.out.print("Supplier Name: ");
            String supplier = scanner.nextLine().trim();
            System.out.print("Unit Price (₹): ");
            double price = Double.parseDouble(scanner.nextLine().trim());
            if (!Double.isFinite(price) || price < 0)
                throw new IllegalArgumentException("Unit price must be zero or greater");

            MedicineBatch batch = new MedicineBatch(id, name, batchNo, qty, mfgDate, expDate, supplier, price);
            if (!DatabaseHelper.saveOrUpdateMedicine(batch)) {
                System.out.println("Could not save to database.");
                return;
            }
            syncFromDatabase();
            System.out.println("Medicine successfully added to registry!");

        } catch (Exception e) {
            System.out.println("Error adding medicine: Invalid input format (" + e.getMessage() + ")");
        }
    }

    // --- Feature 3: Low Stock Alerts ---
    static void checkLowStockAlerts() {
        System.out.println("\n=========================== LOW STOCK ALERTS ===========================");
        System.out.println("[!] Low Stock Warnings (Threshold <= " + LOW_STOCK_THRESHOLD + " units):");
        boolean hasLowStock = false;
        for (int i = 0; i < stockCount; i++) {
            MedicineBatch m = Stock[i];
            if (m.getQuantity() <= LOW_STOCK_THRESHOLD) {
                System.out.printf("  - ID: %s | %s (Batch: %s) -> Current Stock: %d\n",
                        m.getId(), m.getName(), m.getBatchNumber(), m.getQuantity());
                hasLowStock = true;
            }
        }
        if (!hasLowStock)
            System.out.println("  None. All stock levels are sufficient.");
    }

    // --- Feature 4: Expiry Alerts ---
    static void checkExpiryAlerts() {
        LocalDate today = LocalDate.now();
        System.out.println("\n=========================== EXPIRY ALERTS ===========================");
        System.out.println("[!] Expiry & Near-Expiry Alerts (Within " + EXPIRY_WARNING_DAYS + " days):");
        boolean hasExpiryAlerts = false;
        for (int i = 0; i < stockCount; i++) {
            MedicineBatch m = Stock[i];
            long daysUntilExpiry = ChronoUnit.DAYS.between(today, m.getExpiryDate());
            if (daysUntilExpiry < 0) {
                System.out.printf("  - [EXPIRED] ID: %s | %s (Batch: %s) -> Expired %d days ago (%s)\n",
                        m.getId(), m.getName(), m.getBatchNumber(), Math.abs(daysUntilExpiry), m.getExpiryDate());
                hasExpiryAlerts = true;
            } else if (daysUntilExpiry <= EXPIRY_WARNING_DAYS) {
                System.out.printf("  - [CRITICAL] ID: %s | %s (Batch: %s) -> Expires in %d days (%s)\n",
                        m.getId(), m.getName(), m.getBatchNumber(), daysUntilExpiry, m.getExpiryDate());
                hasExpiryAlerts = true;
            }
        }
        if (!hasExpiryAlerts)
            System.out.println("  None. No items nearing expiration.");
    }

    // --- Feature 5: Billing ---
    // Billing only selects items and prepares the pending cart.
    static void createBill(Scanner scanner) {
        if (!pendingBillItems.isEmpty()) {
            System.out.println("A bill is already pending. Complete checkout first.");
            return;
        }

        System.out.println("\n--- Billing: Create Customer Bill ---");
        System.out.print("Customer Name: ");
        pendingCustomerName = scanner.nextLine().trim();

        while (true) {
            System.out.print("Enter Medicine ID to purchase (or 'done' to finish): ");
            String id = scanner.nextLine().trim();
            if (id.equalsIgnoreCase("done"))
                break;

            if (!inventory.containsKey(id)) {
                System.out.println("Invalid Medicine ID.");
                continue;
            }

            MedicineBatch batch = inventory.get(id);

            // Guard: Expired medicines cannot be sold
            if (batch.getExpiryDate().isBefore(LocalDate.now())) {
                System.out.println("Cannot sell this batch! Medicine has expired on " + batch.getExpiryDate());
                continue;
            }

            System.out.print("Available stock: " + batch.getQuantity() + ". Enter quantity: ");
            int qtyToBuy;
            try {
                qtyToBuy = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
                continue;
            }

            if (qtyToBuy <= 0 || qtyToBuy > batch.getQuantity()) {
                System.out.println("Invalid quantity requested.");
                continue;
            }

            SaleItem saleItem = new SaleItem(batch, qtyToBuy);
            pendingBillItems.add(saleItem);
            System.out.println("Item added. Subtotal: ₹" + saleItem.subtotal);
        }

        if (pendingBillItems.isEmpty()) {
            System.out.println("Billing canceled. No items added.");
            pendingCustomerName = null;
            return;
        }

        System.out.println("Billing complete. Proceed to Checkout to finalize the sale.");
        System.out.printf("Pending total: ₹%.2f%n", calculatePendingTotal());
    }

    // --- Feature 6: Checkout ---
    // Checkout deducts stock, records the transaction, and prints the invoice.
    private static void checkout() {
        if (pendingBillItems.isEmpty()) {
            System.out.println("No pending bill. Use Billing first.");
            return;
        }

        for (SaleItem item : pendingBillItems) {
            if (item.quantitySold > item.batch.getQuantity()) {
                System.out.println("Checkout failed: insufficient stock for " + item.batch.getName());
                pendingBillItems.clear();
                pendingCustomerName = null;
                return;
            }
        }

        for (SaleItem item : pendingBillItems) {
            item.batch.setQuantity(item.batch.getQuantity() - item.quantitySold);
        }

        double totalBillAmount = calculatePendingTotal();

        // Generate Invoice
        String billId = "INV-" + (billCounter++);
        TransactionRecord record = new TransactionRecord(billId, pendingCustomerName, LocalDate.now(),
                new ArrayList<>(pendingBillItems), totalBillAmount);
        DatabaseHelper.recordTransaction(record);
        syncFromDatabase();

        System.out.println("\n========================= INVOICE =========================");
        System.out.println("Bill ID: " + billId + " | Date: " + LocalDate.now());
        System.out.println("Customer: " + pendingCustomerName);
        System.out.println("-----------------------------------------------------------");
        System.out.printf("%-16s | %-8s | %-8s | %-10s\n", "Medicine", "Price", "Qty", "Subtotal");
        System.out.println("-----------------------------------------------------------");
        for (SaleItem item : pendingBillItems) {
            System.out.printf("%-16s | ₹%-7.2f | %-8d | ₹%-9.2f\n",
                    item.batch.getName(), item.batch.getUnitPrice(), item.quantitySold, item.subtotal);
        }
        System.out.println("-----------------------------------------------------------");
        System.out.printf("TOTAL PAYABLE: ₹%.2f\n", totalBillAmount);
        System.out.println("===========================================================");

        pendingBillItems.clear();
        pendingCustomerName = null;
    }

    private static double calculatePendingTotal() {
        double total = 0;
        for (SaleItem item : pendingBillItems) {
            total += item.subtotal;
        }
        return total;
    }

    // --- Feature 7: Transaction Logs ---
    static void viewTransactionHistory() {
        System.out.println("\n-------------------------- TRANSACTION HISTORY --------------------------");
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions recorded yet.");
            return;
        }
        for (TransactionRecord t : transactionHistory) {
            System.out.printf("Bill ID: %s | Date: %s | Customer: %s | Total: ₹%.2f\n",
                    t.billId, t.date, t.customerName, t.totalAmount);
            for (SaleItem item : t.items) {
                System.out.printf("   -> %s (Batch: %s) x %d = ₹%.2f\n",
                        item.batch.getName(), item.batch.getBatchNumber(), item.quantitySold, item.subtotal);
            }
            System.out.println("-------------------------------------------------------------------------");
        }
    }

    // --- Mock Data Seed ---
    private static void seedSampleData() {
        addToStock(new MedicineBatch("M001", "Amoxicillin", "B101", 8, LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(25), "PharmaCorp", 12.50));
        addToStock(new MedicineBatch("M002", "Paracetamol", "B102", 50, LocalDate.now().minusMonths(2),
                LocalDate.now().plusMonths(12), "HealthMeds", 3.00));
        addToStock(new MedicineBatch("M003", "Cough Syrup(S)", "B103", 5, LocalDate.now().minusYears(1),
                LocalDate.now().minusDays(5), "SyrupCo", 8.00));
        addToStock(new MedicineBatch("M004", "Ibuprofen", "B104", 20, LocalDate.now().minusMonths(3),
                LocalDate.now().plusMonths(8), "PainRelief Inc.", 5.50));
        addToStock(new MedicineBatch("M005", "Antacid", "B105", 15, LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6), "DigestiveCare", 4.00));
        addToStock(new MedicineBatch("M006", "Vitamin C", "B106", 40, LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(18), "Vitality", 2.50));
        addToStock(new MedicineBatch("M007", "Amoxicillin", "B107", 10, LocalDate.now().plusMonths(2),
                LocalDate.now().plusMonths(10), "PharmaCorp", 12.50));
        addToStock(new MedicineBatch("M008", "Cough Syrup(A)", "B108", 3, LocalDate.now().minusDays(20),
                LocalDate.now().plusDays(10), "SyrupCo", 8.00));
        addToStock(new MedicineBatch("M009", "Paracetamol", "B109", 100, LocalDate.now().minusMonths(4),
                LocalDate.now().plusMonths(15), "HealthMeds", 3.00));
        addToStock(new MedicineBatch("M010", "Pain Relief Gel", "B110", 25, LocalDate.now().minusMonths(2),
                LocalDate.now().plusMonths(5), "TopicalCare", 7.25));
    }

    private static boolean addToStock(MedicineBatch batch) {
        if (stockCount >= Stock.length) {
            return false;
        }
        Stock[stockCount++] = batch;
        inventory.put(batch.getId(), batch);
        return true;
    }

    // --- Swing GUI ---
    static class PharmacyFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        private final DefaultTableModel stockModel = new DefaultTableModel(
                new String[] { "ID", "Medicine", "Batch", "Stock", "Manufactured", "Expiry", "Supplier", "Price" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final DefaultTableModel cartModel = new DefaultTableModel(
                new String[] { "ID", "Medicine", "Qty", "Unit Price", "Subtotal" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final JTable stockTable = new JTable(stockModel);
        private final JTable cartTable = new JTable(cartModel);
        private final JComboBox<String> medicineCombo = new JComboBox<>();
        private final JTextArea alertsArea = new JTextArea();
        // Transaction history list — supports separators + click-to-invoice
        private final DefaultListModel<TransactionRecord> historyListModel = new DefaultListModel<>();
        private final JList<TransactionRecord> historyList = new JList<>(historyListModel);
        private final JLabel status = new JLabel(" Connected to MySQL (pharmacy_db)");

        PharmacyFrame() {
            setTitle("Pharmacy Inventory & Billing System");
            setSize(1100, 680);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (JOptionPane.showConfirmDialog(PharmacyFrame.this, "Exit the pharmacy system?", "Confirm Exit",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        System.exit(0);
                }
            });

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(new EmptyBorder(12, 12, 8, 12));
            setContentPane(root);
            JLabel heading = new JLabel("PHARMACY INVENTORY & BILLING SYSTEM");
            heading.setFont(new Font("SansSerif", Font.BOLD, 22));
            root.add(heading, BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Inventory", inventoryPanel());
            tabs.addTab("Alerts", alertsPanel());
            tabs.addTab("Billing", billingPanel());
            tabs.addTab("Transactions", historyPanel());
            root.add(tabs, BorderLayout.CENTER);
            root.add(status, BorderLayout.SOUTH);
            refreshAll();
        }

        private JPanel inventoryPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton add = new JButton("Add New Medicine");
            JButton refill = new JButton("Refill / Update Stock");
            JButton refresh = new JButton("Refresh");
            add.addActionListener(e -> medicineDialog());
            refill.addActionListener(e -> stockRefillDialog());
            refresh.addActionListener(e -> refreshAll());
            buttons.add(add);
            buttons.add(refill);
            buttons.add(refresh);
            panel.add(buttons, BorderLayout.NORTH);
            stockTable.setAutoCreateRowSorter(true);
            panel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
            return panel;
        }

        private JPanel alertsPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JButton refresh = new JButton("Refresh Alerts");
            refresh.addActionListener(e -> refreshAlerts());
            panel.add(refresh, BorderLayout.NORTH);
            alertsArea.setEditable(false);
            alertsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            panel.add(new JScrollPane(alertsArea), BorderLayout.CENTER);
            return panel;
        }

        private JPanel billingPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField customer = new JTextField(18);
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
            JButton add = new JButton("Add to Bill");
            JButton remove = new JButton("Remove Selected");
            JButton checkoutButton = new JButton("Checkout");
            top.add(new JLabel("Customer:"));
            top.add(customer);
            top.add(new JLabel("Medicine:"));
            top.add(medicineCombo);
            top.add(new JLabel("Quantity:"));
            top.add(quantity);
            top.add(add);
            panel.add(top, BorderLayout.NORTH);
            panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JLabel total = new JLabel("Total: ₹0.00");
            bottom.add(remove);
            bottom.add(total);
            bottom.add(checkoutButton);
            panel.add(bottom, BorderLayout.SOUTH);

            add.addActionListener(e -> {
                if (medicineCombo.getSelectedItem() == null)
                    return;
                String id = medicineCombo.getSelectedItem().toString().split(" - ", 2)[0];
                MedicineBatch b = inventory.get(id);
                int qty = (Integer) quantity.getValue();
                if (b.getExpiryDate().isBefore(LocalDate.now())) {
                    error("This medicine batch has expired.");
                    return;
                }
                int already = 0;
                for (SaleItem item : pendingBillItems)
                    if (item.batch == b)
                        already += item.quantitySold;
                if (qty + already > b.getQuantity()) {
                    error("Quantity exceeds available stock (" + b.getQuantity() + ").");
                    return;
                }
                pendingCustomerName = customer.getText().trim();
                if (pendingCustomerName.isEmpty()) {
                    error("Enter the customer name first.");
                    return;
                }
                pendingBillItems.add(new SaleItem(b, qty));
                refreshCart(total);
            });
            remove.addActionListener(e -> {
                int row = cartTable.getSelectedRow();
                if (row >= 0)
                    pendingBillItems.remove(row);
                refreshCart(total);
            });
            checkoutButton.addActionListener(e -> {
                if (pendingBillItems.isEmpty()) {
                    error("The bill is empty.");
                    return;
                }
                String invoice = invoiceText();
                checkout();
                refreshAll();
                refreshCart(total);
                JTextArea view = new JTextArea(invoice);
                view.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(view), "Invoice", JOptionPane.INFORMATION_MESSAGE);
                customer.setText("");
            });
            return panel;
        }

        private JPanel historyPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));

            // --- Top bar ---
            JPanel topBar = new JPanel(new BorderLayout());
            JLabel hint = new JLabel("  Click a transaction to view its invoice ");
            hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            hint.setForeground(new java.awt.Color(80, 80, 180));
            JButton refresh = new JButton("Refresh History");
            refresh.addActionListener(e -> refreshHistory());
            topBar.add(hint, BorderLayout.WEST);
            topBar.add(refresh, BorderLayout.EAST);
            panel.add(topBar, BorderLayout.NORTH);

            // --- Custom cell renderer: each transaction row + separator ---
            historyList.setCellRenderer(new javax.swing.ListCellRenderer<TransactionRecord>() {
                @Override
                public java.awt.Component getListCellRendererComponent(
                        JList<? extends TransactionRecord> list, TransactionRecord t,
                        int index, boolean isSelected, boolean cellHasFocus) {

                    JPanel cell = new JPanel(new BorderLayout());
                    cell.setOpaque(true);
                    cell.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 4, 10));

                    // Header line: Bill ID | Date | Customer | Total
                    JLabel header = new JLabel(
                        t.billId + "   |   " + t.date + "   |   " + t.customerName
                        + "   |   Total: \u20B9" + String.format("%.2f", t.totalAmount));
                    header.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                    header.setForeground(isSelected
                        ? java.awt.Color.WHITE
                        : new java.awt.Color(30, 80, 160));

                    // Item lines below the header
                    StringBuilder itemLines = new StringBuilder("<html><font face='Monospaced' size='3' color='"
                        + (isSelected ? "#ddddff" : "#444444") + "'>");
                    for (SaleItem si : t.items)
                        itemLines.append("&nbsp;&nbsp;&nbsp;&#8250; ")
                            .append(si.batch.getName())
                            .append(" &times; ").append(si.quantitySold)
                            .append(" &nbsp;=&nbsp; \u20B9")
                            .append(String.format("%.2f", si.subtotal))
                            .append("<br>");
                    itemLines.append("</font></html>");
                    JLabel items = new JLabel(itemLines.toString());

                    // Separator line at bottom
                    JPanel sep = new JPanel();
                    sep.setPreferredSize(new java.awt.Dimension(0, 1));
                    sep.setBackground(new java.awt.Color(180, 200, 230));

                    cell.add(header, BorderLayout.NORTH);
                    cell.add(items, BorderLayout.CENTER);
                    cell.add(sep, BorderLayout.SOUTH);

                    if (isSelected) {
                        cell.setBackground(new java.awt.Color(70, 130, 220));
                    } else {
                        cell.setBackground(index % 2 == 0
                            ? java.awt.Color.WHITE
                            : new java.awt.Color(245, 248, 255));
                    }
                    return cell;
                }
            });

            historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            historyList.setFixedCellHeight(-1); // variable height

            // Click listener -> show invoice dialog
            historyList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx >= 0 && historyListModel.getSize() > 0) {
                        TransactionRecord t = historyListModel.getElementAt(idx);
                        showInvoiceDialog(t);
                    }
                }
            });

            panel.add(new JScrollPane(historyList), BorderLayout.CENTER);
            return panel;
        }

        /** Builds and shows a formatted invoice dialog for a transaction */
        private void showInvoiceDialog(TransactionRecord t) {
            String SEP  = "=".repeat(60);
            String DASH = "-".repeat(60);
            StringBuilder inv = new StringBuilder();
            inv.append(SEP).append("\n");
            inv.append(String.format("%25s INVOICE%n", ""));
            inv.append(SEP).append("\n");
            inv.append(String.format("Bill ID : %-20s Date: %s%n", t.billId, t.date));
            inv.append(String.format("Customer: %s%n", t.customerName));
            inv.append(DASH).append("\n");
            inv.append(String.format("%-22s | %-10s | %-5s | %s%n",
                "Medicine", "Price", "Qty", "Subtotal"));
            inv.append(DASH).append("\n");
            for (SaleItem si : t.items) {
                inv.append(String.format("%-22s | \u20B9%-9.2f | %-5d | \u20B9%.2f%n",
                    si.batch.getName(),
                    si.batch.getUnitPrice(),
                    si.quantitySold,
                    si.subtotal));
            }
            inv.append(DASH).append("\n");
            inv.append(String.format("TOTAL PAYABLE: \u20B9%.2f%n", t.totalAmount));
            inv.append(SEP).append("\n");

            JTextArea area = new JTextArea(inv.toString());
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setEditable(false);
            area.setBackground(new java.awt.Color(30, 30, 30));
            area.setForeground(new java.awt.Color(220, 220, 220));
            area.setMargin(new java.awt.Insets(12, 16, 12, 16));
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new java.awt.Dimension(520, 280));

            JOptionPane.showMessageDialog(this, sp,
                "Invoice — " + t.billId, JOptionPane.PLAIN_MESSAGE);
        }

        private List<MedicineBatch> orderedStock() {
            List<MedicineBatch> result = new ArrayList<>();
            for (int i = 0; i < stockCount; i++)
                result.add(Stock[i]);
            return result;
        }

        private void refreshAll() {
            syncFromDatabase();
            refreshStock();
            refreshAlerts();
            refreshHistory();
        }

        private void refreshStock() {
            stockModel.setRowCount(0);
            medicineCombo.removeAllItems();
            for (MedicineBatch b : orderedStock()) {
                stockModel.addRow(new Object[] { b.getId(), b.getName(), b.getBatchNumber(),
                        b.getQuantity(), b.getManufacturingDate(), b.getExpiryDate(), b.getSupplier(),
                        String.format("₹%.2f", b.getUnitPrice()) });
                medicineCombo.addItem(b.getId() + " - " + b.getName());
            }
        }

        private void refreshAlerts() {
            StringBuilder s = new StringBuilder("LOW STOCK ALERTS (threshold ≤ " + LOW_STOCK_THRESHOLD + ")\n");
            boolean found = false;
            for (MedicineBatch b : orderedStock())
                if (b.getQuantity() <= LOW_STOCK_THRESHOLD) {
                    s.append("• ").append(b.getId()).append(" - ").append(b.getName()).append(": ")
                            .append(b.getQuantity()).append(" units\n");
                    found = true;
                }
            if (!found)
                s.append("None\n");
            s.append("\nEXPIRY ALERTS (within ").append(EXPIRY_WARNING_DAYS).append(" days)\n");
            found = false;
            for (MedicineBatch b : orderedStock()) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
                if (days <= EXPIRY_WARNING_DAYS) {
                    s.append("• ").append(b.getId()).append(" - ").append(b.getName()).append(": ")
                            .append(days < 0 ? "EXPIRED" : "expires in " + days + " days").append(" (")
                            .append(b.getExpiryDate()).append(")\n");
                    found = true;
                }
            }
            if (!found)
                s.append("None\n");
            alertsArea.setText(s.toString());
        }

        private void refreshHistory() {
            historyListModel.clear();
            // Add transactions in reverse order (newest first)
            List<TransactionRecord> sorted = new ArrayList<>(transactionHistory);
            java.util.Collections.reverse(sorted);
            for (TransactionRecord t : sorted)
                historyListModel.addElement(t);
        }

        private void refreshCart(JLabel total) {
            cartModel.setRowCount(0);
            for (SaleItem i : pendingBillItems)
                cartModel.addRow(new Object[] { i.batch.getId(), i.batch.getName(), i.quantitySold,
                        String.format("₹%.2f", i.batch.getUnitPrice()), String.format("₹%.2f", i.subtotal) });
            total.setText(String.format("Total: ₹%.2f", calculatePendingTotal()));
        }

        private void medicineDialog() {
            JTextField id = new JTextField(), name = new JTextField(), batch = new JTextField(), qty = new JTextField(),
                    mfg = new JTextField("" + LocalDate.now()),
                    exp = new JTextField("" + LocalDate.now().plusMonths(12)),
                    supplier = new JTextField(), price = new JTextField();
            Object[] fields = { "Medicine ID (new unique ID):", id, "Medicine name:", name, "Batch number:", batch,
                    "Initial Quantity:", qty, "Manufacturing date (YYYY-MM-DD):", mfg, "Expiry date (YYYY-MM-DD):", exp,
                    "Supplier:", supplier, "Unit price:", price };
            if (JOptionPane.showConfirmDialog(this, fields, "Add New Medicine",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;
            try {
                if (id.getText().trim().isEmpty())
                    throw new IllegalArgumentException("ID is required");
                if (inventory.containsKey(id.getText().trim()))
                    throw new IllegalArgumentException("ID already exists. Use 'Refill / Update Stock' to add quantity to an existing medicine.");
                int amount = Integer.parseInt(qty.getText().trim());
                if (amount <= 0)
                    throw new IllegalArgumentException("Quantity must be positive");
                LocalDate manufacturingDate = LocalDate.parse(mfg.getText().trim(), DATE_FORMAT);
                LocalDate expiryDate = LocalDate.parse(exp.getText().trim(), DATE_FORMAT);
                if (manufacturingDate.isAfter(expiryDate))
                    throw new IllegalArgumentException("Manufacturing date cannot be after expiry date");
                if (expiryDate.isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("New medicine cannot already be expired");
                double unitPrice = Double.parseDouble(price.getText().trim());
                if (!Double.isFinite(unitPrice) || unitPrice < 0)
                    throw new IllegalArgumentException("Unit price must be zero or greater");
                MedicineBatch newBatch = new MedicineBatch(id.getText().trim(), name.getText().trim(),
                        batch.getText().trim(), amount, manufacturingDate, expiryDate,
                        supplier.getText().trim(), unitPrice);
                DatabaseHelper.saveOrUpdateMedicine(newBatch);
                syncFromDatabase();
                refreshAll();
                status.setText(" New medicine '" + name.getText().trim() + "' added to MySQL database.");
            } catch (Exception ex) {
                error("Invalid input: " + ex.getMessage());
            }
        }

        private void stockRefillDialog() {
            if (inventory.isEmpty()) {
                error("No medicines in stock. Add a medicine first.");
                return;
            }

            // Build dropdown from existing medicines
            String[] medicineOptions = orderedStock().stream()
                    .map(b -> b.getId() + " - " + b.getName() + "  [Current Stock: " + b.getQuantity() + " units]")
                    .toArray(String[]::new);

            JComboBox<String> medSelect = new JComboBox<>(medicineOptions);
            medSelect.setPrototypeDisplayValue("MXXX - Medicine Name  [Current Stock: 999 units]");

            JTextField addQtyField = new JTextField("0");
            JTextField newPriceField = new JTextField();
            JTextField newExpiryField = new JTextField();
            JTextField newSupplierField = new JTextField();

            // Update placeholder details when medicine selection changes
            Runnable updateFields = () -> {
                String selected = (String) medSelect.getSelectedItem();
                if (selected == null) return;
                String id = selected.split(" - ")[0].trim();
                MedicineBatch b = inventory.get(id);
                if (b != null) {
                    newPriceField.setText(String.valueOf(b.getUnitPrice()));
                    newExpiryField.setText(b.getExpiryDate().format(DATE_FORMAT));
                    newSupplierField.setText(b.getSupplier());
                }
            };
            medSelect.addActionListener(e -> updateFields.run());
            updateFields.run(); // populate on open

            Object[] fields = {
                    "Select Medicine to Refill:", medSelect,
                    "Quantity to ADD (units):", addQtyField,
                    "Update Unit Price (₹, leave as-is to keep current):", newPriceField,
                    "Update Expiry Date (YYYY-MM-DD, leave as-is to keep current):", newExpiryField,
                    "Update Supplier (leave as-is to keep current):", newSupplierField
            };

            if (JOptionPane.showConfirmDialog(this, fields, "Refill / Update Stock",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;

            try {
                String selected = (String) medSelect.getSelectedItem();
                if (selected == null) return;
                String id = selected.split(" - ")[0].trim();
                MedicineBatch existing = inventory.get(id);
                if (existing == null) throw new IllegalArgumentException("Medicine not found.");

                int addQty = Integer.parseInt(addQtyField.getText().trim());
                if (addQty < 0)
                    throw new IllegalArgumentException("Quantity to add cannot be negative.");

                // Apply updates
                int newQty = existing.getQuantity() + addQty;
                existing.setQuantity(newQty);

                String priceText = newPriceField.getText().trim();
                if (!priceText.isEmpty()) {
                    double newPrice = Double.parseDouble(priceText);
                    if (!Double.isFinite(newPrice) || newPrice < 0)
                        throw new IllegalArgumentException("Price must be zero or greater.");
                    existing.setUnitPrice(newPrice);
                }

                String expiryText = newExpiryField.getText().trim();
                if (!expiryText.isEmpty()) {
                    LocalDate newExpiry = LocalDate.parse(expiryText, DATE_FORMAT);
                    if (newExpiry.isBefore(LocalDate.now()))
                        throw new IllegalArgumentException("Updated expiry date cannot be in the past.");
                    existing.setExpiryDate(newExpiry);
                }

                String supplierText = newSupplierField.getText().trim();
                if (!supplierText.isEmpty()) {
                    existing.setSupplier(supplierText);
                }

                // Persist to MySQL
                DatabaseHelper.saveOrUpdateMedicine(existing);
                syncFromDatabase();
                refreshAll();
                status.setText(" Stock refilled for '" + existing.getName() + "': +" + addQty +
                        " units. New total: " + existing.getQuantity() + " units. Saved to MySQL.");

                JOptionPane.showMessageDialog(this,
                        "Stock Refill Successful!\n\n" +
                        "Medicine   : " + existing.getName() + "\n" +
                        "Units Added: " + addQty + "\n" +
                        "New Stock  : " + existing.getQuantity() + " units\n" +
                        "Unit Price : ₹" + String.format("%.2f", existing.getUnitPrice()) + "\n" +
                        "Expiry Date: " + existing.getExpiryDate() + "\n" +
                        "Supplier   : " + existing.getSupplier() + "\n\n" +
                        "✓ Changes saved to MySQL database.",
                        "Stock Updated", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                error("Stock update failed: " + ex.getMessage());
            }
        }

        private String invoiceText() {
            StringBuilder s = new StringBuilder("Invoice\nCustomer: ").append(pendingCustomerName).append("\nDate: ")
                    .append(LocalDate.now()).append("\n\n");
            for (SaleItem i : pendingBillItems)
                s.append(i.batch.getName()).append(" x ").append(i.quantitySold).append(" = ₹")
                        .append(String.format("%.2f", i.subtotal)).append("\n");
            return s.append("\nTOTAL: ₹").append(String.format("%.2f", calculatePendingTotal())).toString();
        }

        private void error(String message) {
            JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

/** Entry point alias so both 'java Main' and 'java PharmacyManagementSystem' work */
class Main {
    public static void main(String[] args) {
        PharmacyManagementSystem.main(args);
    }
}
