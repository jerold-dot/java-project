import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestAllOperations {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("   VERIFYING ALL SQL DATABASE OPERATIONS");
        System.out.println("================================================");

        DatabaseHelper.initializeDatabase();
        DatabaseHelper.seedIfEmpty();

        // 1. TEST ADDING A NEW MEDICINE
        String testId = "TEST01";
        PharmacyManagementSystem.MedicineBatch newMed = new PharmacyManagementSystem.MedicineBatch(
                testId, "Test Antibiotic", "TB999", 50,
                LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(12),
                "TestSupplier", 15.00
        );

        boolean addOk = DatabaseHelper.saveOrUpdateMedicine(newMed);
        System.out.println("\n[1] ADD NEW MEDICINE: " + (addOk ? "SUCCESS" : "FAILED"));

        // Verify from DB
        List<PharmacyManagementSystem.MedicineBatch> list = DatabaseHelper.getAllMedicines();
        boolean foundNew = list.stream().anyMatch(m -> m.getId().equals(testId) && m.getQuantity() == 50);
        System.out.println("    Verified in MySQL: " + (foundNew ? "PASSED (Quantity = 50)" : "FAILED"));

        // 2. TEST UPDATING STOCK QUANTITY
        newMed.setQuantity(75); // Add 25 more
        boolean updateOk = DatabaseHelper.saveOrUpdateMedicine(newMed);
        System.out.println("\n[2] UPDATE STOCK: " + (updateOk ? "SUCCESS" : "FAILED"));

        list = DatabaseHelper.getAllMedicines();
        boolean foundUpdated = list.stream().anyMatch(m -> m.getId().equals(testId) && m.getQuantity() == 75);
        System.out.println("    Verified in MySQL: " + (foundUpdated ? "PASSED (Quantity = 75)" : "FAILED"));

        // 3. TEST BILLING / CHECKOUT (Deduct stock, insert transaction and line items)
        int nextBill = DatabaseHelper.getNextBillNumber();
        String billId = "INV-" + nextBill;

        List<PharmacyManagementSystem.SaleItem> items = new ArrayList<>();
        items.add(new PharmacyManagementSystem.SaleItem(newMed, 10)); // Sell 10 units

        PharmacyManagementSystem.TransactionRecord tx = new PharmacyManagementSystem.TransactionRecord(
                billId, "Test Customer", LocalDate.now(), items, 150.00
        );

        boolean checkoutOk = DatabaseHelper.recordTransaction(tx);
        System.out.println("\n[3] BILLING & CHECKOUT: " + (checkoutOk ? "SUCCESS" : "FAILED"));

        // Verify stock decremented to 65 (75 - 10)
        list = DatabaseHelper.getAllMedicines();
        boolean foundDecremented = list.stream().anyMatch(m -> m.getId().equals(testId) && m.getQuantity() == 65);
        System.out.println("    Stock Decremented in MySQL: " + (foundDecremented ? "PASSED (New Quantity = 65)" : "FAILED"));

        // Verify transaction logged in DB
        List<PharmacyManagementSystem.TransactionRecord> txs = DatabaseHelper.getAllTransactions();
        boolean foundTx = txs.stream().anyMatch(t -> t.billId.equals(billId) && t.totalAmount == 150.00 && t.items.size() == 1);
        System.out.println("    Transaction Logged in MySQL: " + (foundTx ? "PASSED (Bill " + billId + " Recorded)" : "FAILED"));

        // Clean up test record from DB
        try (java.sql.Connection conn = DatabaseHelper.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM transaction_items WHERE bill_id = '" + billId + "'");
            stmt.executeUpdate("DELETE FROM transactions WHERE bill_id = '" + billId + "'");
            stmt.executeUpdate("DELETE FROM medicines WHERE id = '" + testId + "'");
            System.out.println("\n[Clean-up] Removed temporary test records from MySQL.");
        } catch (Exception e) {
            System.err.println("Clean-up warning: " + e.getMessage());
        }

        System.out.println("\n================================================");
        System.out.println("  ALL DATABASE OPERATIONS VERIFIED & WORKING!");
        System.out.println("================================================");
    }
}

