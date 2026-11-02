# UML Class Diagram — Pharmacy Inventory & Billing Management System

This document describes the UML Class Diagram structure for the Pharmacy Management System (`project.java`).

Both high-resolution vector and raster image files have been generated in this directory:
- **Vector (SVG)**: [`class_diagram.svg`](class_diagram.svg) (Open in any browser or embed directly into Microsoft Word)
- **High-Resolution Image (PNG)**: [`class_diagram.png`](class_diagram.png) (300 DPI high-res image)

---

## Mermaid UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Billable {
        <<interface>>
        +calculateTotalValue() double*
    }

    class MedicineCategory {
        <<abstract>>
        -categoryName: String
        +getCategory() String*
    }

    class MedicineBatch {
        -id: String
        -name: String
        -batchNumber: String
        -quantity: int
        -manufacturingDate: LocalDate
        -expiryDate: LocalDate
        -supplier: String
        -unitPrice: double
        +MedicineBatch(id, name, batchNo, qty, mfg, exp, supplier, price)
        +getCategory() String
        +calculateTotalValue() double
        +getId() String
        +getName() String
        +getQuantity() int
        +setQuantity(quantity: int) void
        +getUnitPrice() double
        +setUnitPrice(unitPrice: double) void
        +setExpiryDate(date: LocalDate) void
        +setSupplier(supplier: String) void
    }

    class SaleItem {
        -batch: MedicineBatch
        -quantitySold: int
        -subtotal: double
        +SaleItem(batch: MedicineBatch, quantitySold: int)
    }

    class TransactionRecord {
        -billId: String
        -customerName: String
        -date: LocalDate
        -items: List~SaleItem~
        -totalAmount: double
        +TransactionRecord(billId, customer, date, items, total)
    }

    class DatabaseHelper {
        -DB_URL: String
        -USER: String
        -PASSWORD: String
        +getConnection() Connection
        +initializeDatabase() void
        +seedIfEmpty() void
        +saveOrUpdateMedicine(m: MedicineBatch) boolean
        +getAllMedicines() List~MedicineBatch~
        +updateStock(id: String, newQty: int) void
        +saveTransaction(t: TransactionRecord) boolean
        +getAllTransactions() List~TransactionRecord~
        +getNextBillNumber() int
    }

    class PharmacyManagementSystem {
        -inventory: Map~String, MedicineBatch~
        -stock: MedicineBatch[]
        -transactionHistory: List~TransactionRecord~
        -pendingBillItems: List~SaleItem~
        +syncFromDatabase() void
        +seedSampleData() void
        +main(args: String[]) void
    }

    class PharmacyFrame {
        -stockTable: JTable
        -cartTable: JTable
        -historyList: JList~TransactionRecord~
        -alertsArea: JTextArea
        +PharmacyFrame()
        +inventoryPanel() JPanel
        +billingPanel() JPanel
        +historyPanel() JPanel
        +alertsPanel() JPanel
        +stockRefillDialog() void
        +showInvoiceDialog(t: TransactionRecord) void
        +refreshAll() void
    }

    %% Relationships
    MedicineCategory <|-- MedicineBatch : extends
    Billable <|.. MedicineBatch : implements
    MedicineBatch "1" <-- "0..*" SaleItem : references
    TransactionRecord "1" *-- "1..*" SaleItem : contains
    DatabaseHelper ..> MedicineBatch : persists / reads
    DatabaseHelper ..> TransactionRecord : persists / loads
    PharmacyManagementSystem o-- MedicineBatch : manages
    PharmacyManagementSystem o-- TransactionRecord : logs
    PharmacyFrame ..> PharmacyManagementSystem : interacts
```

---

## Diagram Relationships Explanation

1. **Generalization (`extends`)**:
   - `MedicineBatch` inherits from the abstract base class `MedicineCategory` and overrides `getCategory()`.
2. **Realization (`implements`)**:
   - `MedicineBatch` implements the `Billable` interface and overrides `calculateTotalValue()`.
3. **Association**:
   - `SaleItem` holds a reference to the `MedicineBatch` sold.
4. **Composition / Aggregation**:
   - `TransactionRecord` is composed of multiple `SaleItem` objects (`List<SaleItem>`), representing the items in a bill.
5. **Dependency / Persistence**:
   - `DatabaseHelper` handles CRUD persistence for `MedicineBatch` (table `medicines`) and `TransactionRecord` (tables `transactions` and `transaction_items`).
6. **GUI Interaction**:
   - `PharmacyFrame` (inheriting from `JFrame`) renders the presentation layer and delegates data operations to `PharmacyManagementSystem` and `DatabaseHelper`.
