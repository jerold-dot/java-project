import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    // Database connection parameters (can be overridden via environment variables)
    private static final String HOST = System.getenv("MYSQL_HOST") != null ? System.getenv("MYSQL_HOST") : "localhost:3306";
    private static final String HOST_URL = "jdbc:mysql://" + HOST + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_URL = "jdbc:mysql://" + HOST + "/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = System.getenv("MYSQL_USER") != null ? System.getenv("MYSQL_USER") : "root";
    private static final String PASSWORD = System.getenv("MYSQL_PASSWORD") != null ? System.getenv("MYSQL_PASSWORD") : "JerolD@#1705";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /**
     * Creates database and tables automatically if they do not exist.
     */
    public static void initializeDatabase() {
        // 1. Create database if it doesn't exist
        try (Connection conn = DriverManager.getConnection(HOST_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS pharmacy_db");
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Notice on DB creation: " + e.getMessage());
        }

        // 2. Create tables
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
            System.err.println("[DatabaseHelper] Table creation warning: " + e.getMessage());
        }
    }

    /**
     * Seeds initial sample medicines if the medicines table is empty.
     */
    public static void seedIfEmpty() {
        String check = "SELECT COUNT(*) FROM medicines";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {

            if (rs.next() && rs.getInt(1) == 0) {
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M001", "Amoxicillin", "B101", 8,
                        LocalDate.now().minusMonths(6), LocalDate.now().plusDays(25), "PharmaCorp", 12.50));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M002", "Paracetamol", "B102", 50,
                        LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(12), "HealthMeds", 3.00));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M003", "Cough Syrup(S)", "B103", 5,
                        LocalDate.now().minusYears(1), LocalDate.now().minusDays(5), "SyrupCo", 8.00));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M004", "Ibuprofen", "B104", 20,
                        LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(8), "PainRelief Inc.", 5.50));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M005", "Antacid", "B105", 15,
                        LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), "DigestiveCare", 4.00));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M006", "Vitamin C", "B106", 40,
                        LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18), "Vitality", 2.50));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M007", "Amoxicillin", "B107", 10,
                        LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(10), "PharmaCorp", 12.50));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M008", "Cough Syrup(A)", "B108", 3,
                        LocalDate.now().minusDays(20), LocalDate.now().plusDays(10), "SyrupCo", 8.00));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M009", "Paracetamol", "B109", 100,
                        LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(15), "HealthMeds", 3.00));
                saveOrUpdateMedicine(new PharmacyManagementSystem.MedicineBatch("M010", "Pain Relief Gel", "B110", 25,
                        LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(5), "TopicalCare", 7.25));
                System.out.println("[DatabaseHelper] Sample medicine records seeded into MySQL.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Seed check error: " + e.getMessage());
        }
    }

    /**
     * Fetches all medicines from MySQL database.
     */
    public static List<PharmacyManagementSystem.MedicineBatch> getAllMedicines() {
        List<PharmacyManagementSystem.MedicineBatch> list = new ArrayList<>();
        String query = "SELECT id, name, batch_number, quantity, mfg_date, exp_date, supplier, unit_price FROM medicines";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(new PharmacyManagementSystem.MedicineBatch(
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

    /**
     * Inserts new medicine or updates quantity if medicine ID exists.
     */
    public static boolean saveOrUpdateMedicine(PharmacyManagementSystem.MedicineBatch batch) {
        String query = "INSERT INTO medicines (id, name, batch_number, quantity, mfg_date, exp_date, supplier, unit_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";

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

    /**
     * Atomically records a checkout transaction, inserts items, and updates stock in MySQL.
     */
    public static boolean recordTransaction(PharmacyManagementSystem.TransactionRecord record) {
        String insertTx = "INSERT INTO transactions (bill_id, customer_name, bill_date, total_amount) VALUES (?, ?, ?, ?)";
        String insertItem = "INSERT INTO transaction_items (bill_id, medicine_id, quantity_sold, subtotal) VALUES (?, ?, ?, ?)";
        String updateStock = "UPDATE medicines SET quantity = quantity - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert transaction header
            try (PreparedStatement psTx = conn.prepareStatement(insertTx)) {
                psTx.setString(1, record.billId);
                psTx.setString(2, record.customerName);
                psTx.setDate(3, java.sql.Date.valueOf(record.date));
                psTx.setDouble(4, record.totalAmount);
                psTx.executeUpdate();
            }

            // 2. Insert items and decrement stock
            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(updateStock)) {

                for (PharmacyManagementSystem.SaleItem item : record.items) {
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

            conn.commit(); // Commit all changes
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[DatabaseHelper] Rollback failed: " + ex.getMessage());
                }
            }
            System.err.println("[DatabaseHelper] Error recording transaction: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }
    }

    /**
     * Loads all transactions and their respective line items from MySQL.
     */
    public static List<PharmacyManagementSystem.TransactionRecord> getAllTransactions() {
        List<PharmacyManagementSystem.TransactionRecord> list = new ArrayList<>();
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

                List<PharmacyManagementSystem.SaleItem> items = new ArrayList<>();
                try (PreparedStatement psItems = conn.prepareStatement(queryItems)) {
                    psItems.setString(1, billId);
                    try (ResultSet rsItem = psItems.executeQuery()) {
                        while (rsItem.next()) {
                            PharmacyManagementSystem.MedicineBatch batch = new PharmacyManagementSystem.MedicineBatch(
                                    rsItem.getString("medicine_id"),
                                    rsItem.getString("name"),
                                    rsItem.getString("batch_number"),
                                    rsItem.getInt("quantity"),
                                    rsItem.getDate("mfg_date").toLocalDate(),
                                    rsItem.getDate("exp_date").toLocalDate(),
                                    rsItem.getString("supplier"),
                                    rsItem.getDouble("unit_price")
                            );
                            items.add(new PharmacyManagementSystem.SaleItem(batch, rsItem.getInt("quantity_sold")));
                        }
                    }
                }
                list.add(new PharmacyManagementSystem.TransactionRecord(billId, customerName, date, items, totalAmount));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Error loading transactions: " + e.getMessage());
        }
        return list;
    }
}

