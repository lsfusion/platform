package fdk.integration;



import java.sql.Timestamp;
import java.util.Date;

public class UserInvoiceDetail {
    public String number;
    public String series;
    public Boolean createPricing;
    public Boolean createShipment;
    public String sid;
    public Date date;
    public String itemID;
    public Boolean isWare;
    public Double quantity;
    public String supplier;
    public String customerWarehouse;
    public String supplierWarehouse;
    public Double price;
    public Double chargePrice;
    public Double manufacturingPrice;
    public Double wholesalePrice;
    public Double wholesaleMarkup;
    public Double retailPrice;
    public Double retailMarkup;
    public String textCompliance;
    public String contractID;


    public UserInvoiceDetail(String number, String series, Boolean createPricing, Boolean createShipment, String sid,
                             Date date, String itemID, Boolean isWare, Double quantity, String supplier, String customerWarehouse,
                             String supplierWarehouse, Double price, Double chargePrice, Double manufacturingPrice,
                             Double wholesalePrice, Double wholesaleMarkup, Double retailPrice, Double retailMarkup,
                             String textCompliance, String contractID) {
        this.number = number;
        this.series = series;
        this.createPricing = createPricing;
        this.createShipment = createShipment;
        this.sid = sid;
        this.date = date;
        this.itemID = itemID;
        this.isWare = isWare;
        this.quantity = quantity;
        this.supplier = supplier;
        this.customerWarehouse = customerWarehouse;
        this.supplierWarehouse = supplierWarehouse;
        this.price = price;
        this.chargePrice = chargePrice;
        this.manufacturingPrice = manufacturingPrice;
        this.wholesalePrice = wholesalePrice;
        this.wholesaleMarkup = wholesaleMarkup;
        this.retailPrice = retailPrice;
        this.retailMarkup = retailMarkup;
        this.textCompliance = textCompliance;
        this.contractID = contractID;
    }
}
