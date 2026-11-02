package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.CategoryDao;
import com.goldenchemist.pos.dao.MedicineDao;
import com.goldenchemist.pos.model.Category;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.math.BigDecimal;
import java.util.List;

public class MedicineController {

    private final MedicineDao dao = new MedicineDao();
    private final CategoryDao categoryDao = new CategoryDao();

    public List<Medicine> search(String q) {
        Session.require(Module.MEDICINES);
        return dao.search(q, false);
    }

    public List<Category> categories() {
        return categoryDao.findAll();
    }

    public void addCategory(String name) {
        Session.require(Module.MEDICINES);
        categoryDao.insert(Strings.required("Category name", name, 60));
    }

    /** Validates and inserts (id == 0) or updates a medicine. */
    public void save(Medicine m) {
        Session.require(Module.MEDICINES);
        m.setBarcode(Strings.required("Barcode", m.getBarcode(), 40));
        m.setBrandName(Strings.required("Brand name", m.getBrandName(), 100));
        m.setGenericName(Strings.optional("Generic name", m.getGenericName(), 100));
        m.setDosageForm(Strings.optional("Dosage form", m.getDosageForm(), 30));
        m.setStrength(Strings.optional("Strength", m.getStrength(), 30));
        m.setManufacturer(Strings.optional("Manufacturer", m.getManufacturer(), 100));
        if (m.getSellingPrice() == null || m.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PosException("Selling price must be greater than zero.");
        }
        if (m.getReorderLevel() < 0) {
            throw new PosException("Reorder level cannot be negative.");
        }
        if (m.getId() == 0) {
            dao.insert(m);
        } else {
            dao.update(m);
        }
    }

    public void setActive(Medicine m, boolean active) {
        Session.require(Module.MEDICINES);
        dao.setActive(m.getId(), active);
    }
}
