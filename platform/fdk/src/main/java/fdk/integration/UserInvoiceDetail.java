package fdk.integration;


import java.util.Date;

public class UserInvoiceDetail {
    public String number;
    public String series;
    public Boolean createPricing;
    public Boolean createShipment;
    public String sid;
    public Date date;
    public String item;
    public Double quantity;
    public String supplier;
    public String warehouse;
    public String supplierWarehouse;
    public Double price;
    public Double retailPrice;
    public Double retailMarkup;

    public UserInvoiceDetail(String number, String series, Boolean createPricing, Boolean createShipment, String sid,
                             Date date, String item, Double quantity, String supplier, String warehouse,
                             String supplierWarehouse, Double price, Double retailPrice, Double retailMarkup) {
        this.number = number;
        this.series = series;
        this.createPricing = createPricing;
        this.createShipment = createShipment;
        this.sid = sid;
        this.date = date;
        this.item = item;
        this.quantity = quantity;
        this.supplier = supplier;
        this.warehouse = warehouse;
        this.supplierWarehouse = supplierWarehouse;
        this.price = price;
        this.retailPrice = retailPrice;
        this.retailMarkup = retailMarkup;

    }
}
