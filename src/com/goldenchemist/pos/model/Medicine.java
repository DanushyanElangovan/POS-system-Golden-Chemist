package com.goldenchemist.pos.model;

import java.math.BigDecimal;

public class Medicine {
    private int id;
    private String barcode;
    private String brandName;
    private String genericName;
    private Integer categoryId;
    private String categoryName;
    private String dosageForm;
    private String strength;
    private String manufacturer;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private int reorderLevel = 10;
    private boolean requiresPrescription;
    private boolean active = true;
    /** Sellable (not expired) stock across all batches; filled by queries. */
    private int stockQty;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public boolean isLowStock() { return stockQty <= reorderLevel; }

    @Override
    public String toString() { return brandName + (strength == null ? "" : " " + strength); }
}
