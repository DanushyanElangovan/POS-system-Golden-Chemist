-- =====================================================================
--  Sample data (optional). Run AFTER schema.sql:
--     mysql -u root -p golden_chemist_pos < database/seed.sql
--  Prices and products are illustrative only - replace with real stock.
--  The first administrator (admin / admin123) is created by the app on first start.
-- =====================================================================
USE golden_chemist_pos;

INSERT IGNORE INTO category (name) VALUES
 ('Analgesics & Antipyretics'),('Antibiotics'),('Antihistamines & Allergy'),('Gastrointestinal'),
 ('Diabetes'),('Cardiovascular'),('Respiratory'),('Vitamins & Supplements'),
 ('Topical & First Aid'),('Baby & Personal Care');

INSERT IGNORE INTO supplier (name, contact_person, phone, email, address) VALUES
 ('Ceylon Pharma Distributors','Nimal Perera','0112345678','orders@ceylonpharma.example','Colombo 10'),
 ('Lanka Medi Supplies','Shanika Fernando','0112765432','sales@lankamedi.example','Colombo 12'),
 ('Island Health Wholesale','Kasun Silva','0312223344','info@islandhealth.example','Negombo');

INSERT IGNORE INTO customer (full_name, phone) VALUES
 ('Sample Customer One','0771234567'),('Sample Customer Two','0719876543');

INSERT IGNORE INTO medicine (barcode, brand_name, generic_name, category_id, dosage_form, strength, manufacturer, selling_price, reorder_level, requires_prescription) VALUES
 ('4790001000011','Paracetamol','Paracetamol',(SELECT category_id FROM category WHERE name='Analgesics & Antipyretics'),'Tablet','500mg','Generic',3.00,200,0),
 ('4790001000028','Diclofenac Gel','Diclofenac',(SELECT category_id FROM category WHERE name='Analgesics & Antipyretics'),'Gel','1%','Generic',420.00,10,0),
 ('4790001000035','Amoxicillin','Amoxicillin',(SELECT category_id FROM category WHERE name='Antibiotics'),'Capsule','500mg','Generic',18.00,100,1),
 ('4790001000042','Azithromycin','Azithromycin',(SELECT category_id FROM category WHERE name='Antibiotics'),'Tablet','500mg','Generic',95.00,30,1),
 ('4790001000059','Cetirizine','Cetirizine',(SELECT category_id FROM category WHERE name='Antihistamines & Allergy'),'Tablet','10mg','Generic',6.00,100,0),
 ('4790001000066','Omeprazole','Omeprazole',(SELECT category_id FROM category WHERE name='Gastrointestinal'),'Capsule','20mg','Generic',12.00,100,0),
 ('4790001000073','ORS Sachet','Oral rehydration salts',(SELECT category_id FROM category WHERE name='Gastrointestinal'),'Sachet','27.9g','Generic',45.00,50,0),
 ('4790001000080','Metformin','Metformin',(SELECT category_id FROM category WHERE name='Diabetes'),'Tablet','500mg','Generic',5.50,200,1),
 ('4790001000097','Amlodipine','Amlodipine',(SELECT category_id FROM category WHERE name='Cardiovascular'),'Tablet','5mg','Generic',7.00,150,1),
 ('4790001000103','Salbutamol Inhaler','Salbutamol',(SELECT category_id FROM category WHERE name='Respiratory'),'Inhaler','100mcg','Generic',780.00,10,1),
 ('4790001000110','Cough Syrup','Dextromethorphan',(SELECT category_id FROM category WHERE name='Respiratory'),'Syrup','100ml','Generic',390.00,15,0),
 ('4790001000127','Vitamin C','Ascorbic acid',(SELECT category_id FROM category WHERE name='Vitamins & Supplements'),'Tablet','500mg','Generic',8.00,100,0),
 ('4790001000134','Povidone Iodine','Povidone iodine',(SELECT category_id FROM category WHERE name='Topical & First Aid'),'Solution','10%','Generic',260.00,10,0),
 ('4790001000141','Adhesive Plaster','',(SELECT category_id FROM category WHERE name='Topical & First Aid'),'Other','100 pcs','Generic',350.00,10,0);

-- Batches use dates relative to today so the demo always has valid, expiring and low stock.
INSERT IGNORE INTO batch (medicine_id, supplier_id, batch_no, expiry_date, quantity, cost_price, received_date)
SELECT m.medicine_id, (SELECT supplier_id FROM supplier ORDER BY supplier_id LIMIT 1), 'B-2401',
       DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 500, ROUND(m.selling_price*0.70,2), CURDATE()
FROM medicine m WHERE m.barcode IN ('4790001000011','4790001000059','4790001000066','4790001000127','4790001000080','4790001000097');

INSERT IGNORE INTO batch (medicine_id, supplier_id, batch_no, expiry_date, quantity, cost_price, received_date)
SELECT m.medicine_id, (SELECT supplier_id FROM supplier ORDER BY supplier_id LIMIT 1 OFFSET 1), 'B-2402',
       DATE_ADD(CURDATE(), INTERVAL 14 MONTH), 60, ROUND(m.selling_price*0.72,2), CURDATE()
FROM medicine m WHERE m.barcode IN ('4790001000028','4790001000035','4790001000042','4790001000073','4790001000110','4790001000134','4790001000141');

-- Near-expiry batch (FEFO will sell this one first) and a low-stock item
INSERT IGNORE INTO batch (medicine_id, supplier_id, batch_no, expiry_date, quantity, cost_price, received_date)
SELECT medicine_id, 2, 'B-2310', DATE_ADD(CURDATE(), INTERVAL 25 DAY), 40, 2.10, CURDATE() FROM medicine WHERE barcode='4790001000011';
INSERT IGNORE INTO batch (medicine_id, supplier_id, batch_no, expiry_date, quantity, cost_price, received_date)
SELECT medicine_id, 3, 'B-2403', DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 4, 540.00, CURDATE() FROM medicine WHERE barcode='4790001000103';
