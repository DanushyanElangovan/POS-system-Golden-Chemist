package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.dao.BatchDao;
import com.goldenchemist.pos.dao.DashboardDao;
import com.goldenchemist.pos.dao.MedicineDao;
import com.goldenchemist.pos.model.Batch;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.Module;

import java.math.BigDecimal;
import java.util.List;

public class DashboardController {

    private final DashboardDao dao = new DashboardDao();
    private final MedicineDao medicineDao = new MedicineDao();
    private final BatchDao batchDao = new BatchDao();

    /** Plain holder for the dashboard cards. */
    public static class Summary {
        public BigDecimal todaySales;
        public long todayInvoices;
        public long activeMedicines;
        public long lowStock;
        public long expiringSoon;
        public long expired;
    }

    public Summary summary() {
        Session.require(Module.DASHBOARD);
        Summary s = new Summary();
        s.todaySales = dao.todaySalesTotal();
        s.todayInvoices = dao.todayInvoiceCount();
        s.activeMedicines = dao.activeMedicineCount();
        s.lowStock = dao.lowStockCount();
        s.expiringSoon = dao.expiringCount(AppConfig.expiryWarningDays());
        s.expired = dao.expiredWithStockCount();
        return s;
    }

    public List<Medicine> lowStockItems() {
        Session.require(Module.DASHBOARD);
        return medicineDao.lowStock(12);
    }

    public List<Batch> expiringBatches() {
        Session.require(Module.DASHBOARD);
        return batchDao.expiringSoon(AppConfig.expiryWarningDays(), 12);
    }
}
