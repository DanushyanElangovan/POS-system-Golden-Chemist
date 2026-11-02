package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.dao.BatchDao;
import com.goldenchemist.pos.dao.CustomerDao;
import com.goldenchemist.pos.dao.MedicineDao;
import com.goldenchemist.pos.dao.SaleDao;
import com.goldenchemist.pos.model.CartLine;
import com.goldenchemist.pos.model.Customer;
import com.goldenchemist.pos.model.Discount;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.model.Role;
import com.goldenchemist.pos.model.Sale;
import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Owns the shopping cart of the billing screen and completes sales. */
public class BillingController {

    private final MedicineDao medicineDao = new MedicineDao();
    private final BatchDao batchDao = new BatchDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final SaleDao saleDao = new SaleDao();
    private final List<CartLine> cart = new ArrayList<>();

    // ---- searching -------------------------------------------------------------------------

    public List<Medicine> searchMedicines(String q) {
        Session.require(Module.BILLING);
        return medicineDao.search(q, true);
    }

    /** Exact barcode match (barcode scanner), or null. */
    public Medicine findByBarcode(String barcode) {
        Session.require(Module.BILLING);
        return Strings.isBlank(barcode) ? null : medicineDao.findByBarcode(barcode);
    }

    public List<Customer> customers() {
        Session.require(Module.BILLING);
        return customerDao.search("");
    }

    // ---- cart ------------------------------------------------------------------------------

    public List<CartLine> getCart() {
        return cart;
    }

    public void add(Medicine medicine, int qty) {
        Session.require(Module.BILLING);
        if (qty <= 0) {
            throw new PosException("Quantity must be at least 1.");
        }
        int available = batchDao.availableQty(medicine.getId());
        CartLine existing = find(medicine.getId());
        int wanted = qty + (existing == null ? 0 : existing.getQuantity());
        if (available <= 0) {
            throw new PosException(medicine + " is out of stock (expired batches cannot be sold).");
        }
        if (wanted > available) {
            throw new PosException("Only " + available + " unit(s) of " + medicine + " are available.");
        }
        if (existing == null) {
            cart.add(new CartLine(medicine, qty, available));
        } else {
            existing.setQuantity(wanted);
            existing.setAvailable(available);
        }
    }

    public void setQuantity(int index, int qty) {
        Session.require(Module.BILLING);
        CartLine line = cart.get(index);
        if (qty <= 0) {
            cart.remove(index);
            return;
        }
        int available = batchDao.availableQty(line.getMedicine().getId());
        if (qty > available) {
            throw new PosException("Only " + available + " unit(s) of " + line.getMedicine() + " are available.");
        }
        line.setQuantity(qty);
        line.setAvailable(available);
    }

    public void remove(int index) {
        cart.remove(index);
    }

    public void clear() {
        cart.clear();
    }

    private CartLine find(int medicineId) {
        for (CartLine l : cart) {
            if (l.getMedicine().getId() == medicineId) {
                return l;
            }
        }
        return null;
    }

    // ---- totals ----------------------------------------------------------------------------

    public BigDecimal subtotal() {
        BigDecimal s = BigDecimal.ZERO;
        for (CartLine l : cart) {
            s = s.add(l.getLineTotal());
        }
        return Money.scale(s);
    }

    public boolean needsPrescription() {
        for (CartLine l : cart) {
            if (l.getMedicine().isRequiresPrescription()) {
                return true;
            }
        }
        return false;
    }

    /** Parses the discount typed at the till and applies the role limit. */
    public Discount parseDiscount(String text, Discount.Type type) {
        if (Strings.isBlank(text)) {
            return Discount.NONE;
        }
        BigDecimal v = Money.parse("Discount", text);
        if (v.signum() < 0) {
            throw new PosException("Discount cannot be negative.");
        }
        Discount d = new Discount(type, v);
        BigDecimal sub = subtotal();
        if (type == Discount.Type.PERCENT && v.compareTo(new BigDecimal("100")) > 0) {
            throw new PosException("A percentage discount cannot exceed 100%.");
        }
        if (type == Discount.Type.AMOUNT && v.compareTo(sub) > 0) {
            throw new PosException("The discount cannot be more than the subtotal.");
        }
        if (Session.user().getRole() == Role.CASHIER) {
            BigDecimal max = AppConfig.getDecimal("cashier.max.discount.percent", "10");
            if (d.percentOf(sub).compareTo(max) > 0) {
                throw new PosException("Cashiers can give at most " + max.stripTrailingZeros().toPlainString()
                        + "% discount. Ask a pharmacist or administrator.");
            }
        }
        return d;
    }

    // ---- checkout --------------------------------------------------------------------------

    /**
     * Completes the sale. paymentMethod is CASH or CARD; tenderedText is the cash handed over
     * (ignored for card). The cart is cleared on success.
     */
    public Sale checkout(Customer customer, Discount discount, String paymentMethod, String tenderedText,
                         String prescriptionRef) {
        Session.require(Module.BILLING);
        if (cart.isEmpty()) {
            throw new PosException("The cart is empty.");
        }
        String rx = Strings.optional("Prescription reference", prescriptionRef, 60);
        if (needsPrescription() && rx == null) {
            throw new PosException("Enter the prescription reference (doctor / Rx number) - the cart contains "
                    + "prescription-only medicine.");
        }
        BigDecimal tendered = null;
        if ("CASH".equals(paymentMethod)) {
            tendered = Money.parse("Amount received", tenderedText);
        }
        Sale sale = saleDao.checkout(new ArrayList<>(cart), customer == null ? null : customer.getId(),
                Session.user().getId(), discount == null ? Discount.NONE : discount, paymentMethod, tendered, rx);
        cart.clear();
        return sale;
    }
}
