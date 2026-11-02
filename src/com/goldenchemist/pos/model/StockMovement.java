package com.goldenchemist.pos.model;

import java.time.LocalDateTime;

public class StockMovement {
    private int id;
    private String medicineName;
    private String batchNo;
    private int changeQty;
    private String reason;
    private String reference;
    private String employeeName;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public int getChangeQty() { return changeQty; }
    public void setChangeQty(int changeQty) { this.changeQty = changeQty; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
