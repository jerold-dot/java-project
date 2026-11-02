# Database Entity-Relationship (ER) Diagram — Pharmacy Inventory & Billing System

This document outlines the relational database architecture and Entity-Relationship (ER) specifications for the **Pharmacy Inventory & Billing Management System** (`pharmacy_db`).

Both high-resolution vector and raster image files have been generated in this directory:
- **Vector Graphic (SVG)**: [`database_er_diagram.svg`](database_er_diagram.svg) (Open in any web browser or embed directly into Microsoft Word without quality loss)
- **High-Resolution Image (PNG)**: [`database_er_diagram.png`](database_er_diagram.png) (300 DPI high-res image suitable for reports and presentations)

---

## Mermaid ER Diagram

```mermaid
erDiagram
    SUPPLIERS ||--o{ MEDICINES : supplies
    MEDICINES ||--o{ INVENTORY_ALERTS : triggers
    MEDICINES ||--o{ TRANSACTION_ITEMS : ordered_in
    TRANSACTIONS ||--|{ TRANSACTION_ITEMS : contains
    PHARMACY_USERS ||--o{ TRANSACTIONS : processes
    PHARMACY_USERS ||--o{ AUDIT_LOG : recorded_in

    SUPPLIERS {
        varchar(10) supplier_id PK
        varchar(100) supplier_name
        varchar(100) contact_person
        varchar(20) phone
        varchar(100) email
    }

    MEDICINES {
        varchar(10) id PK
        varchar(100) name
        varchar(50) batch_number
        int quantity
        date mfg_date
        date exp_date
        varchar(100) supplier
        decimal unit_price
    }

    TRANSACTIONS {
        varchar(20) bill_id PK
        varchar(100) customer_name
        date bill_date
        decimal total_amount
    }

    TRANSACTION_ITEMS {
        int item_id PK
        varchar(20) bill_id FK
        varchar(10) medicine_id FK
        int quantity_sold
        decimal subtotal
    }

    INVENTORY_ALERTS {
        int alert_id PK
        varchar(10) medicine_id FK
        varchar(20) alert_type
        int threshold_value
        varchar(20) status
    }

    PHARMACY_USERS {
        int user_id PK
        varchar(50) username UK
        varchar(255) password_hash
        varchar(100) full_name
        varchar(20) role
    }

    AUDIT_LOG {
        bigint log_id PK
        int user_id FK
        varchar(50) action
        varchar(50) target_table
        datetime timestamp
    }

    SETTINGS {
        varchar(50) setting_key PK
        varchar(100) setting_value
        varchar(255) description
    }
```

---

## Entity & Relationship Specifications

### 1. Active MySQL Tables (`setup_pharmacy_db.sql`)

| Table Name | Primary Key | Foreign Keys | Description |
|---|---|---|---|
| **`medicines`** | `id VARCHAR(10)` | None | Stores pharmaceutical inventory batches, stock quantities, manufacturing and expiration dates, and pricing. |
| **`transactions`** | `bill_id VARCHAR(20)` | None | Stores billing records, customer name, sale date, and total invoice amount. |
| **`transaction_items`** | `item_id INT AUTO_INCREMENT` | `bill_id` &rarr; `transactions(bill_id)`<br>`medicine_id` &rarr; `medicines(id)` | Bridge / line-item entity recording each medicine, quantity purchased, and subtotal per transaction. |

### 2. Relational Cardinalities
- **`transactions` (1) to `transaction_items` (N)**: One invoice contains one or more line items (`ON DELETE CASCADE`).
- **`medicines` (1) to `transaction_items` (N)**: A medicine batch can be referenced across multiple sales.
- **`medicines` (1) to `inventory_alerts` (N)**: Automatic threshold warnings triggered when stock &le; 10 units or expiring within 30 days.
- **`suppliers` (1) to `medicines` (N)**: Pharmaceutical manufacturers/distributors supplying medication batches.
