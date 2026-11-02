# POS System — Golden Chemist Pharmacy

Desktop Point of Sale system for a retail pharmacy, built with **Java, Java Swing, MySQL and Jasper Reports**
on an **MVC architecture**. It covers **billing, inventory and employee management**.

## What it does

| Area | Features |
|---|---|
| **Billing** | Barcode scan / search, cart with live totals, FEFO stock allocation (earliest expiry sold first, expired stock never sold), prescription reference required for Rx-only medicine, % or amount discount (cashiers capped at 10 %), cash / card, change calculation, printable A5 invoice, F2 / F9 shortcuts |
| **Invoices** | Search by number / customer / date, re-print, void with reason (stock is restored) |
| **Medicines** | Barcode, brand + generic name, category, dosage form, strength, price, reorder level, Rx flag, active flag |
| **Inventory** | Stock by **batch** with expiry (amber = expiring soon, red = expired), receive stock from suppliers, manual adjustment / write-off with reason, full stock-movement history |
| **Employees** | Administrator / Pharmacist / Cashier roles, salted PBKDF2 passwords, lock-out after 5 failed log-ins, cannot remove the last admin or your own admin role |
| **Suppliers, Customers** | Maintain contacts; quick-add a customer from the till |
| **Dashboard** | Today's sales, invoices, low stock, expiring and expired counts, reorder list |
| **Reports (Jasper)** | Invoice, Sales, Top-selling medicines, Sales by employee, Low stock, Expiring/expired stock, Stock valuation — preview, PDF or Excel (.xls) |

Role access: **Administrator** everything · **Pharmacist** everything except Employees · **Cashier** Dashboard, Billing, Invoices (own), Customers.
Access is enforced in the controllers, not only by hiding menu items.

## Quick start

1. Install **JDK 8 or newer** and **MySQL 5.7 / 8 or MariaDB 10** (XAMPP is fine).
2. Create the database:
   ```
   mysql -u root -p < database/schema.sql
   mysql -u root -p golden_chemist_pos < database/seed.sql      (optional sample medicines)
   ```
3. Edit `config/app.properties` — database user/password and your shop name, address and phone (printed on invoices).
4. Build and run:
   * Linux / macOS: `sh build.sh` then `sh run.sh`
   * Windows: `build.bat` then `run.bat`
5. First start signs in with **admin / admin123** — change it under *Change password*, then add staff under *Employees*.

Always start the program from the project folder (it reads `config/` and `reports/` relative to it).
In NetBeans / IntelliJ / Eclipse: create a project from existing sources, add every jar in `lib/`, main class `com.goldenchemist.pos.App`, working directory = project folder.

## Choosing the JDBC driver  (important)

`lib/` contains **MariaDB Connector/J 2.7.6**, which accepts `jdbc:mysql://` URLs. It was tested against MariaDB 10.11.

* The old driver in the original project (MySQL Connector/J **5.1.22**, 2012) fails to connect to current MariaDB servers, and is known to have problems with MySQL 8, so it is **not** included.
* **MySQL 8 users:** the default `caching_sha2_password` login may not work with the bundled driver. Either
  (a) download **MySQL Connector/J 8.x** from mysql.com, delete `lib/mariadb-java-client-2.7.6.jar` and put the new jar in `lib/` (no code change needed), or
  (b) create the database user with `IDENTIFIED WITH mysql_native_password`.
* If the app says the driver cannot talk to the server, this is the reason.

## Architecture (MVC)

```
src/com/goldenchemist/pos/
  App.java                 entry point
  model/                   Employee, Medicine, Batch, Sale, SaleItem, CartLine, Discount, Role, Module ...
  view/                    Swing screens: LoginView, MainFrame, BillingPanel, MedicinePanel, InventoryPanel, ...
                           (AbstractCrudPanel = reusable table + form screen)
  controller/              validation, business rules, role checks: BillingController, InventoryController, ...
  dao/                     all SQL (PreparedStatements only): SaleDao, BatchDao, MedicineDao, ...
  db/                      Database (connections, transactions), Sql helpers
  report/                  ReportService (compile / fill / export Jasper), CompileReports (build helper)
  config/, util/           AppConfig, Money (BigDecimal), PasswordUtil, Strings, PosException
reports/                   7 Jasper templates (.jrxml); compiled to reports/compiled/ by the build
database/                  schema.sql, seed.sql
test/                      SmokeTest (headless end-to-end test)
```

Views never contain SQL; controllers never touch Swing. A sale is **one database transaction**: prices are re-read from the
database, batches are locked and drawn down in expiry order, every change is written to `stock_movement`, and the invoice number is created from the sale id.

## Testing

`POS_CONFIG=config/app.properties sh test.sh` runs 37 checks against your database (log-in, roles, FEFO, prescription rule,
discount cap, void + restock, all reports to PDF/Excel). It adds a few test rows, so run it on a practice database.

## Not included (ideas for later)

Credit sales / customer balances, purchase orders, sales returns of single items, thermal-receipt layout, barcode label printing, backup scheduling.
