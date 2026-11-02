package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.dao.BatchDao;
import com.goldenchemist.pos.dao.MedicineDao;
import com.goldenchemist.pos.dao.SupplierDao;
import com.goldenchemist.pos.model.Batch;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.model.StockMovement;
import com.goldenchemist.pos.model.Supplier;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryController {

    private final BatchDao batchDao = new BatchDao();
    private final MedicineDao medicineDao = new MedicineDao();
    private final SupplierDao supplierDao = new SupplierDao();

    public List<Batch> batches(String q, String filter) {
        Session.require(Module.INVENTORY);
        return batchDao.search(q, filter, AppConfig.expiryWarningDays());
    }

    public List<StockMovement> movements() {
        Session.require(Module.INVENTORY);
        return batchDao.recentMovements(300);
    }

    public List<Medicine> activeMedicines() {
        Session.require(Module.INVENTORY);
        return medicineDao.search("", true);
    }

    public List<Supplier> activeSuppliers() {
        Session.require(Module.INVENTORY);
        return supplierDao.search("", true);
    }

    /** Records goods received from a supplier as a new batch. */
    public void receive(Medicine medicine, Supplier supplier, String batchNo, LocalDate expiry,
                        int qty, BigDecimal cost, LocalDate received) {
        Session.require(Module.INVENTORY);
        if (medicine == null) {
            throw new PosException("Choose a medicine.");
        }
        Batch b = new Batch();
        b.setMedicineId(medicine.getId());
        b.setSupplierId(supplier == null ? null : supplier.getId());
        b.setBatchNo(Strings.required("Batch number", batchNo, 40));
        if (expiry == null || !expiry.isAfter(LocalDate.now())) {
            throw new PosException("The expiry date must be in the future - expired stock cannot be received.");
        }
        if (qty <= 0) {
            throw new PosException("Quantity must be at least 1.");
        }
        if (cost == null || cost.signum() < 0) {
            throw new PosException("Cost price cannot be negative.");
        }
        b.setExpiryDate(expiry);
        b.setQuantity(qty);
        b.setCostPrice(cost);
        b.setReceivedDate(received == null ? LocalDate.now() : received);
        batchDao.receive(b, Session.user().getId());
    }

    /** reason is ADJUSTMENT (count correction) or WRITE_OFF (damaged / expired stock removed). */
    public void adjust(Batch batch, int delta, String reason, String note) {
        Session.require(Module.INVENTORY);
        if (batch == null) {
            throw new PosException("Select a batch first.");
        }
        if (delta == 0) {
            throw new PosException("Enter a quantity change other than zero.");
        }
        if ("WRITE_OFF".equals(reason) && delta > 0) {
            throw new PosException("A write-off must reduce stock.");
        }
        if (Strings.isBlank(note)) {
            throw new PosException("Please give a reason for the change.");
        }
        batchDao.adjust(batch.getId(), delta, reason, note.trim(), Session.user().getId());
    }
}
