package com.goldenchemist.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Batch {
    private int id;
    private int medicineId;
    private String medicineName;
    private Integer supplierId;
    private String supplierName;
    private String batchNo;
    private LocalDate expiryDate;
    private int quantity;
    private BigDecimal costPrice = BigDecimal.ZERO;
    private LocalDate receivedDate = LocalDate.now();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public boolean isExpired() { return expiryDate != null && expiryDate.isBefore(LocalDate.now()); }

    public long daysToExpiry() { return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate); }
}
