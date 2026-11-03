import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

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

        // Constructor 1: Full parameterized constructor
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

        // Constructor 2: Overloaded constructor (convenience with default 1-year shelf life)
        public MedicineBatch(String id, String name, String batchNumber, int quantity, double unitPrice) {
            this(id, name, batchNumber, quantity, LocalDate.now(), LocalDate.now().plusYears(1), "Standard Pharma", unitPrice);
        }

        @Override
        public String getCategory() {
            return "Pharmacy Medicine";
        }

        // Method 1: calculateTotalValue without discount
        @Override
        public double calculateTotalValue() {
            return quantity * unitPrice;
        }

        // Method Overloading: calculateTotalValue with discount percentage
        public double calculateTotalValue(double discountPercent) {
            double baseTotal = calculateTotalValue();
            return baseTotal - (baseTotal * (discountPercent / 100.0));
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
        // --- Apply Nimbus Look & Feel with custom pharmacy theme colours ---
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Nimbus colour overrides — deep navy + teal accent
        UIManager.put("nimbusBase",               new Color(22, 40, 70));
        UIManager.put("nimbusBlueGrey",           new Color(45, 62, 98));
        UIManager.put("control",                  new Color(242, 245, 250));
        UIManager.put("nimbusFocus",              new Color(0, 188, 212));
        UIManager.put("nimbusSelectionBackground",new Color(0, 150, 180));
        UIManager.put("Table.background",         new Color(255, 255, 255));
        UIManager.put("Table.alternateRowColor",  new Color(235, 245, 252));
        UIManager.put("Table[Enabled+Selected].textForeground", Color.WHITE);
        UIManager.put("TabbedPane.background",    new Color(30, 50, 85));
        UIManager.put("TabbedPane.foreground",    new Color(220, 235, 255));
        UIManager.put("TabbedPane.selected",      new Color(0, 188, 212));
        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);

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

    // Wrapper for billing dropdown: encapsulates medicine details, expiry status, and visual formatting
    static class MedicineComboItem {
        final MedicineBatch batch;
        final boolean expired;

        public MedicineComboItem(MedicineBatch batch) {
            this.batch = batch;
            this.expired = batch.getExpiryDate().isBefore(LocalDate.now());
        }

        @Override
        public String toString() {
            if (expired) {
                return "\u25CF [EXPIRED] " + batch.getId() + " - " + batch.getName() + " (Exp: " + batch.getExpiryDate() + ")";
            } else {
                return "\u25CF " + batch.getId() + " - " + batch.getName() + " [Stock: " + batch.getQuantity() + " | \u20B9" + String.format("%.2f", batch.getUnitPrice()) + "]";
            }
        }
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
        // Billing dropdown with type-search and expiry status indicators
        private final JComboBox<MedicineComboItem> medicineCombo = new JComboBox<>();
        private final JTextField searchMedicineField = new JTextField(12);
        private final JLabel expiryBadge = new JLabel();
        private final JTextArea alertsArea = new JTextArea();
        // Transaction history list — supports separators + click-to-invoice
        private final DefaultListModel<TransactionRecord> historyListModel = new DefaultListModel<>();
        private final JList<TransactionRecord> historyList = new JList<>(historyListModel);
        private final JLabel status = new JLabel(" Connected to MySQL (pharmacy_db)");

        PharmacyFrame() {
            setTitle("Pharmacy Inventory & Billing System");
            setSize(1200, 740);
            setMinimumSize(new Dimension(900, 600));
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (JOptionPane.showConfirmDialog(PharmacyFrame.this,
                            "Exit the pharmacy system?", "Confirm Exit",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        System.exit(0);
                }
            });

            // ---- Root Panel (dark navy background) ----
            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(new Color(18, 30, 54));
            setContentPane(root);

            // ---- Top Header Bar ----
            JPanel header = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setPaint(new GradientPaint(0, 0, new Color(10, 20, 50),
                            getWidth(), 0, new Color(0, 80, 120)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            header.setOpaque(false);
            header.setBorder(new EmptyBorder(14, 22, 14, 22));

            // Left: App title + icon
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            titlePanel.setOpaque(false);
            JLabel icon = new JLabel("\u2695");   // medical cross unicode
            icon.setFont(new Font("SansSerif", Font.BOLD, 30));
            icon.setForeground(new Color(0, 200, 220));
            JLabel heading = new JLabel("Pharmacy Inventory & Billing System");
            heading.setFont(new Font("SansSerif", Font.BOLD, 22));
            heading.setForeground(Color.WHITE);
            JLabel subheading = new JLabel("  \u2014  pharmacy_db  |  MySQL 8.0");
            subheading.setFont(new Font("SansSerif", Font.PLAIN, 12));
            subheading.setForeground(new Color(140, 180, 220));
            titlePanel.add(icon);
            titlePanel.add(heading);
            titlePanel.add(subheading);
            header.add(titlePanel, BorderLayout.WEST);

            // Right: status dot
            JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            statusRight.setOpaque(false);
            status.setForeground(new Color(0, 230, 180));
            status.setFont(new Font("SansSerif", Font.BOLD, 12));
            JLabel dot = new JLabel("\u25CF");
            dot.setForeground(new Color(0, 230, 118));
            dot.setFont(new Font("SansSerif", Font.PLAIN, 18));
            statusRight.add(dot);
            statusRight.add(status);
            header.add(statusRight, BorderLayout.EAST);

            root.add(header, BorderLayout.NORTH);

            // ---- Tabs ----
            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
            tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
            tabs.setBackground(new Color(22, 36, 65));
            tabs.setForeground(new Color(200, 220, 255));
            tabs.setBorder(new EmptyBorder(4, 10, 0, 10));

            tabs.addTab("\u2302  Inventory", applyTabStyle(inventoryPanel()));
            tabs.addTab("\u26A0  Alerts", applyTabStyle(alertsPanel()));
            tabs.addTab("\uD83D\uDCC3  Billing", applyTabStyle(billingPanel()));
            tabs.addTab("\uD83D\uDCCA  Transactions", applyTabStyle(historyPanel()));

            // Tab icons via colour
            tabs.setBackgroundAt(0, new Color(25, 118, 210));
            tabs.setBackgroundAt(1, new Color(230, 74, 25));
            tabs.setBackgroundAt(2, new Color(46, 125, 50));
            tabs.setBackgroundAt(3, new Color(106, 27, 154));

            root.add(tabs, BorderLayout.CENTER);

            // ---- Bottom Status Bar ----
            JPanel footerBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
            footerBar.setBackground(new Color(15, 23, 45));
            footerBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0, 100, 150)));
            JLabel footerLabel = new JLabel("  \u00A9 2026 Pharmacy Inventory & Billing System  |  Java 17 + MySQL 8.0  |  All Rights Reserved");
            footerLabel.setForeground(new Color(100, 130, 175));
            footerLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            footerBar.add(footerLabel);
            root.add(footerBar, BorderLayout.SOUTH);

            // ---- Style tables ----
            styleTable(stockTable);
            styleTable(cartTable);

            refreshAll();
        }

        /** Wraps a panel in a styled card with dark background */
        private JPanel applyTabStyle(JPanel inner) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(24, 38, 68));
            card.setBorder(new EmptyBorder(12, 14, 12, 14));
            card.add(inner, BorderLayout.CENTER);
            return card;
        }

        /** Applies modern table styling: alternating rows, custom header */
        private void styleTable(JTable table) {
            table.setRowHeight(28);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setSelectionBackground(new Color(0, 150, 200));
            table.setSelectionForeground(Color.WHITE);
            table.setBackground(Color.WHITE);
            table.setForeground(new Color(30, 40, 60));
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));

            // Header styling
            JTableHeader tableHeader = table.getTableHeader();
            tableHeader.setBackground(new Color(22, 60, 105));
            tableHeader.setForeground(Color.WHITE);
            tableHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
            tableHeader.setPreferredSize(new Dimension(0, 34));
            tableHeader.setBorder(BorderFactory.createEmptyBorder());

            // Alternating row renderer
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(235, 245, 255));
                        c.setForeground(new Color(30, 40, 60));
                    }
                    ((JLabel) c).setBorder(new EmptyBorder(0, 10, 0, 10));
                    return c;
                }
            });
        }

        /** Creates a styled flat button */
        private JButton styledButton(String text, Color bgColor, Color textColor) {
            JButton btn = new JButton(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isPressed() ? bgColor.darker() :
                            getModel().isRollover() ? bgColor.brighter() : bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
                @Override protected void paintBorder(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor.darker());
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                    g2.dispose();
                }
            };
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setForeground(textColor);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(7, 16, 7, 16));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.repaint(); }
                public void mouseExited(MouseEvent e) { btn.repaint(); }
            });
            return btn;
        }

        /** Creates a styled card-style panel */
        private JPanel cardPanel(JComponent content) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(255, 255, 255));
            card.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 215, 235), 1, true),
                new EmptyBorder(10, 10, 10, 10)
            ));
            card.add(content, BorderLayout.CENTER);
            return card;
        }

        /** Creates a section title label */
        private JLabel sectionLabel(String text) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            lbl.setForeground(new Color(200, 220, 255));
            lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
            return lbl;
        }

        /** Wraps scroll pane in a card */
        private JScrollPane styledScroll(JComponent comp) {
            JScrollPane sp = new JScrollPane(comp);
            sp.setBorder(BorderFactory.createLineBorder(new Color(190, 210, 235), 1));
            sp.getViewport().setBackground(Color.WHITE);
            return sp;
        }

        private JPanel inventoryPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);

            // Section header + action buttons
            JPanel topBar = new JPanel(new BorderLayout(0, 4));
            topBar.setOpaque(false);
            topBar.add(sectionLabel("📦  Medicine Inventory"), BorderLayout.NORTH);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            buttons.setOpaque(false);
            JButton add     = styledButton("+ Add Medicine",    new Color(25, 118, 210), Color.WHITE);
            JButton refill  = styledButton("↑ Refill Stock",   new Color(46, 125, 50),  Color.WHITE);
            JButton refresh = styledButton("↻ Refresh",        new Color(55, 71, 79),   Color.WHITE);
            add.addActionListener(e -> medicineDialog());
            refill.addActionListener(e -> stockRefillDialog());
            refresh.addActionListener(e -> refreshAll());
            buttons.add(add);
            buttons.add(refill);
            buttons.add(refresh);
            topBar.add(buttons, BorderLayout.CENTER);

            panel.add(topBar, BorderLayout.NORTH);
            stockTable.setAutoCreateRowSorter(true);
            panel.add(styledScroll(stockTable), BorderLayout.CENTER);
            return panel;
        }

        private JPanel alertsPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);

            JPanel topBar = new JPanel(new BorderLayout(0, 4));
            topBar.setOpaque(false);
            topBar.add(sectionLabel("⚠  Inventory Alerts"), BorderLayout.NORTH);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow.setOpaque(false);
            JButton refresh = styledButton("↻ Refresh Alerts", new Color(180, 50, 20), Color.WHITE);
            refresh.addActionListener(e -> refreshAlerts());
            btnRow.add(refresh);
            topBar.add(btnRow, BorderLayout.CENTER);
            panel.add(topBar, BorderLayout.NORTH);

            alertsArea.setEditable(false);
            alertsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            alertsArea.setBackground(new Color(24, 38, 68));
            alertsArea.setForeground(new Color(200, 230, 255));
            alertsArea.setCaretColor(new Color(0, 200, 220));
            alertsArea.setBorder(new EmptyBorder(10, 14, 10, 14));
            panel.add(styledScroll(alertsArea), BorderLayout.CENTER);
            return panel;
        }

        private JPanel billingPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);

            // Custom Renderer for medicine dropdown with Red/Green dot indicators
            medicineCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof MedicineComboItem) {
                        MedicineComboItem item = (MedicineComboItem) value;
                        if (item.expired) {
                            label.setText("<html><font color='#d91e18' size='4'>&#9679;</font> "
                                    + "<font color='#c0392b'><b>[EXPIRED]</b> "
                                    + item.batch.getId() + " - " + item.batch.getName()
                                    + " (Exp: " + item.batch.getExpiryDate() + ")</font></html>");
                            if (!isSelected) {
                                label.setBackground(new Color(255, 238, 238));
                            }
                        } else {
                            label.setText("<html><font color='#27ae60' size='4'>&#9679;</font> "
                                    + item.batch.getId() + " - " + item.batch.getName()
                                    + " <font color='#555555'>[Stock: " + item.batch.getQuantity()
                                    + " | \u20B9" + String.format("%.2f", item.batch.getUnitPrice()) + "]</font></html>");
                            if (!isSelected) {
                                label.setBackground(Color.WHITE);
                            }
                        }
                    }
                    return label;
                }
            });

            medicineCombo.addActionListener(e -> updateExpiryBadge());

            // Real-time Type-Searching listener
            searchMedicineField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filter(); }
                public void removeUpdate(DocumentEvent e) { filter(); }
                public void changedUpdate(DocumentEvent e) { filter(); }
                private void filter() {
                    filterBillingMedicines(searchMedicineField.getText().trim());
                }
            });

            // ---- Section Header ----
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(sectionLabel("\uD83D\uDCC3  Billing & Checkout"), BorderLayout.WEST);
            panel.add(header, BorderLayout.NORTH);

            // ---- Top Controls (2 rows) ----
            JPanel top = new JPanel(new GridLayout(2, 1, 4, 6));
            top.setOpaque(false);
            top.setBorder(new EmptyBorder(6, 0, 6, 0));

            // Row 1: Customer Name + Search Field
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
            row1.setOpaque(false);
            JTextField customer = new JTextField(16);
            customer.setFont(new Font("SansSerif", Font.PLAIN, 12));
            JButton clearSearch = styledButton("\u2715 Clear", new Color(90, 90, 90), Color.WHITE);
            clearSearch.addActionListener(e -> {
                searchMedicineField.setText("");
                filterBillingMedicines("");
                searchMedicineField.requestFocus();
            });

            JLabel custLbl = new JLabel("Customer:");
            custLbl.setForeground(new Color(200, 220, 255));
            custLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            JLabel srchLbl = new JLabel("     \uD83D\uDD0D Search Medicine:");
            srchLbl.setForeground(new Color(200, 220, 255));
            srchLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            searchMedicineField.setToolTipText("Type medicine name or ID to filter list instantly");
            searchMedicineField.setFont(new Font("SansSerif", Font.PLAIN, 12));
            row1.add(custLbl);
            row1.add(customer);
            row1.add(srchLbl);
            row1.add(searchMedicineField);
            row1.add(clearSearch);

            // Row 2: Medicine Combo + Expiry Badge + Qty Spinner + Add button
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
            row2.setOpaque(false);
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
            JButton add = styledButton("+ Add to Bill",    new Color(46, 125, 50),  Color.WHITE);
            JButton remove = styledButton("\u2715 Remove",       new Color(183, 28, 28), Color.WHITE);
            JButton checkoutButton = styledButton("\uD83E\uDDFE Checkout", new Color(25, 118, 210), Color.WHITE);

            medicineCombo.setPreferredSize(new Dimension(380, 28));
            medicineCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
            JLabel selLbl = new JLabel("Select Medicine:");
            selLbl.setForeground(new Color(200, 220, 255));
            selLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            JLabel qtyLbl = new JLabel("Quantity:");
            qtyLbl.setForeground(new Color(200, 220, 255));
            qtyLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            row2.add(selLbl);
            row2.add(medicineCombo);
            row2.add(expiryBadge);
            row2.add(qtyLbl);
            row2.add(quantity);
            row2.add(add);

            top.add(row1);
            top.add(row2);

            // Wrap controls in a dark card
            JPanel topCard = new JPanel(new BorderLayout());
            topCard.setBackground(new Color(20, 33, 60));
            topCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(0, 100, 160), 1, true),
                new EmptyBorder(8, 10, 8, 10)
            ));
            topCard.add(top, BorderLayout.CENTER);
            panel.add(topCard, BorderLayout.NORTH);

            // ---- Cart Table (center) ----
            panel.add(styledScroll(cartTable), BorderLayout.CENTER);

            // ---- Bottom: Remove + Total + Checkout ----
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
            bottom.setOpaque(false);
            JLabel total = new JLabel("Total: \u20B90.00");
            total.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            total.setForeground(new Color(0, 200, 180));
            bottom.add(remove);
            bottom.add(total);
            bottom.add(checkoutButton);
            panel.add(bottom, BorderLayout.SOUTH);

            // ---- Action Listeners ----
            add.addActionListener(e -> {
                Object sel = medicineCombo.getSelectedItem();
                if (sel == null) {
                    error("Please select a medicine first.");
                    return;
                }
                MedicineBatch b = ((MedicineComboItem) sel).batch;
                int qty = (Integer) quantity.getValue();
                if (b.getExpiryDate().isBefore(LocalDate.now())) {
                    error("Cannot add to bill: '" + b.getName() + "' (Batch " + b.getBatchNumber()
                            + ") EXPIRED on " + b.getExpiryDate() + "!\nExpired medicines cannot be sold.");
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
                    customer.requestFocus();
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
            panel.setOpaque(false);

            // --- Top bar ---
            JPanel topBar = new JPanel(new BorderLayout(0, 4));
            topBar.setOpaque(false);
            topBar.add(sectionLabel("\uD83D\uDCCA  Transaction History"), BorderLayout.NORTH);

            JPanel btnRow = new JPanel(new BorderLayout());
            btnRow.setOpaque(false);
            JLabel hint = new JLabel("  Click a transaction to view its invoice ");
            hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            hint.setForeground(new Color(140, 200, 255));
            JButton refresh = styledButton("↻ Refresh History", new Color(106, 27, 154), Color.WHITE);
            refresh.addActionListener(e -> refreshHistory());
            btnRow.add(hint, BorderLayout.WEST);
            btnRow.add(refresh, BorderLayout.EAST);
            topBar.add(btnRow, BorderLayout.CENTER);
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
                        : new java.awt.Color(0, 188, 212));

                    // Item lines below the header
                    StringBuilder itemLines = new StringBuilder("<html><font face='Monospaced' size='3' color='"
                        + (isSelected ? "#ddddff" : "#9ec5e8") + "'>");
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
                    sep.setBackground(new java.awt.Color(0, 80, 120));

                    cell.add(header, BorderLayout.NORTH);
                    cell.add(items, BorderLayout.CENTER);
                    cell.add(sep, BorderLayout.SOUTH);

                    if (isSelected) {
                        cell.setBackground(new java.awt.Color(0, 110, 160));
                    } else {
                        cell.setBackground(index % 2 == 0
                            ? new java.awt.Color(24, 38, 68)
                            : new java.awt.Color(20, 30, 56));
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

            panel.add(styledScroll(historyList), BorderLayout.CENTER);
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
            for (MedicineBatch b : orderedStock()) {
                stockModel.addRow(new Object[] { b.getId(), b.getName(), b.getBatchNumber(),
                        b.getQuantity(), b.getManufacturingDate(), b.getExpiryDate(), b.getSupplier(),
                        String.format("\u20B9%.2f", b.getUnitPrice()) });
            }
            filterBillingMedicines(searchMedicineField.getText().trim());
        }

        private void filterBillingMedicines(String query) {
            medicineCombo.removeAllItems();
            String q = query.toLowerCase();
            for (MedicineBatch b : orderedStock()) {
                if (q.isEmpty() || b.getName().toLowerCase().contains(q) || b.getId().toLowerCase().contains(q)) {
                    medicineCombo.addItem(new MedicineComboItem(b));
                }
            }
            if (medicineCombo.getItemCount() > 0) {
                medicineCombo.setSelectedIndex(0);
            }
            updateExpiryBadge();
        }

        private void updateExpiryBadge() {
            Object sel = medicineCombo.getSelectedItem();
            if (sel instanceof MedicineComboItem) {
                MedicineComboItem item = (MedicineComboItem) sel;
                if (item.expired) {
                    expiryBadge.setText("<html><font color='#d91e18'><b>&#9679; EXPIRED (" + item.batch.getExpiryDate() + ")</b></font></html>");
                    expiryBadge.setToolTipText("Warning: Expired batch cannot be billed!");
                } else {
                    expiryBadge.setText("<html><font color='#27ae60'><b>&#9679; Active</b></font> <font color='#555555'>(" + item.batch.getQuantity() + " in stock)</font></html>");
                    expiryBadge.setToolTipText("Active batch ready for dispensing.");
                }
            } else {
                expiryBadge.setText("");
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
