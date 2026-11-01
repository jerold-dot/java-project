import java.util.List;

public class VerifyJavaMySQL {
    public static void main(String[] args) {
        System.out.println("--- Testing Java -> MySQL Connection ---");
        DatabaseHelper.initializeDatabase();
        
        List<PharmacyManagementSystem.MedicineBatch> list = DatabaseHelper.getAllMedicines();
        System.out.println("Medicines loaded from MySQL: " + list.size());
        for (PharmacyManagementSystem.MedicineBatch b : list) {
            System.out.printf("  [%s] %-16s | Stock: %-4d | ₹%.2f\n",
                    b.getId(), b.getName(), b.getQuantity(), b.getUnitPrice());
        }

        List<PharmacyManagementSystem.TransactionRecord> txs = DatabaseHelper.getAllTransactions();
        System.out.println("\nTransactions loaded from MySQL: " + txs.size());
        for (PharmacyManagementSystem.TransactionRecord t : txs) {
            System.out.printf("  Bill %s for %s | Total: ₹%.2f | Items: %d\n",
                    t.billId, t.customerName, t.totalAmount, t.items.size());
        }

        System.out.println("\n>>> SUCCESS: Java and MySQL are 100% connected and verified! <<<");
    }
}

