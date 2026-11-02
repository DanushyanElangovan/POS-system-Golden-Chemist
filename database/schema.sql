-- =====================================================================
--  Golden Chemist Pharmacy POS - database schema
--  Works on MySQL 5.7 / 8.x and MariaDB 10.x
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================
CREATE DATABASE IF NOT EXISTS golden_chemist_pos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE golden_chemist_pos;

CREATE TABLE IF NOT EXISTS employee (
  employee_id   INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  username      VARCHAR(40)  NOT NULL UNIQUE,
  password_hash VARCHAR(200) NOT NULL,
  role          ENUM('ADMIN','PHARMACIST','CASHIER') NOT NULL,
  phone         VARCHAR(20),
  email         VARCHAR(100),
  nic           VARCHAR(20),
  hired_date    DATE,
  active        TINYINT(1) NOT NULL DEFAULT 1,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS category (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier (
  supplier_id    INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL UNIQUE,
  contact_person VARCHAR(100),
  phone          VARCHAR(20),
  email          VARCHAR(100),
  address        VARCHAR(200),
  active         TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer (
  customer_id INT AUTO_INCREMENT PRIMARY KEY,
  full_name   VARCHAR(100) NOT NULL,
  phone       VARCHAR(20),
  email       VARCHAR(100),
  address     VARCHAR(200),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicine (
  medicine_id           INT AUTO_INCREMENT PRIMARY KEY,
  barcode               VARCHAR(40)  NOT NULL UNIQUE,
  brand_name            VARCHAR(100) NOT NULL,
  generic_name          VARCHAR(100),
  category_id           INT NULL,
  dosage_form           VARCHAR(30),
  strength              VARCHAR(30),
  manufacturer          VARCHAR(100),
  selling_price         DECIMAL(10,2) NOT NULL,
  reorder_level         INT NOT NULL DEFAULT 10,
  requires_prescription TINYINT(1) NOT NULL DEFAULT 0,
  active                TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_med_cat FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS batch (
  batch_id      INT AUTO_INCREMENT PRIMARY KEY,
  medicine_id   INT NOT NULL,
  supplier_id   INT NULL,
  batch_no      VARCHAR(40) NOT NULL,
  expiry_date   DATE NOT NULL,
  quantity      INT NOT NULL DEFAULT 0,
  cost_price    DECIMAL(10,2) NOT NULL DEFAULT 0,
  received_date DATE NOT NULL,
  UNIQUE KEY uq_batch (medicine_id, batch_no),
  KEY ix_batch_expiry (expiry_date),
  CONSTRAINT fk_batch_med FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
  CONSTRAINT fk_batch_sup FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale (
  sale_id          INT AUTO_INCREMENT PRIMARY KEY,
  invoice_no       VARCHAR(30) NOT NULL UNIQUE,
  sale_date        DATETIME NOT NULL,
  employee_id      INT NOT NULL,
  customer_id      INT NULL,
  subtotal         DECIMAL(12,2) NOT NULL DEFAULT 0,
  discount         DECIMAL(12,2) NOT NULL DEFAULT 0,
  total            DECIMAL(12,2) NOT NULL DEFAULT 0,
  paid_amount      DECIMAL(12,2) NOT NULL DEFAULT 0,
  change_amount    DECIMAL(12,2) NOT NULL DEFAULT 0,
  payment_method   ENUM('CASH','CARD') NOT NULL,
  status           ENUM('COMPLETED','VOIDED') NOT NULL DEFAULT 'COMPLETED',
  prescription_ref VARCHAR(60),
  void_reason      VARCHAR(200),
  voided_by        INT NULL,
  voided_at        DATETIME NULL,
  KEY ix_sale_date (sale_date),
  CONSTRAINT fk_sale_emp  FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
  CONSTRAINT fk_sale_cus  FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE SET NULL,
  CONSTRAINT fk_sale_void FOREIGN KEY (voided_by)   REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_item (
  sale_item_id INT AUTO_INCREMENT PRIMARY KEY,
  sale_id      INT NOT NULL,
  medicine_id  INT NOT NULL,
  batch_id     INT NOT NULL,
  quantity     INT NOT NULL,
  unit_price   DECIMAL(10,2) NOT NULL,
  line_total   DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_si_sale  FOREIGN KEY (sale_id)     REFERENCES sale(sale_id) ON DELETE CASCADE,
  CONSTRAINT fk_si_med   FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
  CONSTRAINT fk_si_batch FOREIGN KEY (batch_id)    REFERENCES batch(batch_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_movement (
  movement_id INT AUTO_INCREMENT PRIMARY KEY,
  batch_id    INT NOT NULL,
  change_qty  INT NOT NULL,
  reason      ENUM('RECEIVE','SALE','VOID','ADJUSTMENT','WRITE_OFF') NOT NULL,
  reference   VARCHAR(100),
  employee_id INT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY ix_mov_created (created_at),
  CONSTRAINT fk_mov_batch FOREIGN KEY (batch_id)    REFERENCES batch(batch_id),
  CONSTRAINT fk_mov_emp   FOREIGN KEY (employee_id) REFERENCES employee(employee_id) ON DELETE SET NULL
) ENGINE=InnoDB;
