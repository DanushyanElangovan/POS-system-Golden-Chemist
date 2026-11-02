package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.SaleDao;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.model.Sale;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.time.LocalDate;
import java.util.List;

public class InvoiceController {

    private final SaleDao dao = new SaleDao();

    /** Cashiers only see their own invoices; pharmacists and admins see all. */
    public List<Sale> search(String q, LocalDate from, LocalDate to, String status) {
        Session.require(Module.INVOICES);
        if (from == null || to == null || to.isBefore(from)) {
            throw new PosException("Choose a valid date range.");
        }
        Integer only = Session.user().getRole().canVoidSales() ? null : Session.user().getId();
        return dao.search(q, from, to, status, only);
    }

    public void voidSale(Sale sale, String reason) {
        Session.require(Module.INVOICES);
        if (!Session.user().getRole().canVoidSales()) {
            throw new PosException("Only a pharmacist or administrator can void an invoice.");
        }
        if (sale == null) {
            throw new PosException("Select an invoice first.");
        }
        if (Strings.isBlank(reason)) {
            throw new PosException("Please enter the reason for voiding this invoice.");
        }
        dao.voidSale(sale.getId(), reason.trim(), Session.user().getId());
    }
}
