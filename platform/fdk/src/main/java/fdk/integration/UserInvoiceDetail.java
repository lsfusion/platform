package fdk.integration;



import java.math.BigDecimal;
import java.util.Date;

public class UserInvoiceDetail {
    public String number;
    public String series;
    public Boolean createPricing;
    public Boolean createShipment;
    public String sid;
    public Date date;
    public String idItem;
    public Boolean isWare;
    public BigDecimal quantity;
    public String supplier;
    public String customerWarehouse;
    public String supplierWarehouse;
    public BigDecimal price;
    public BigDecimal chargePrice;
    public BigDecimal manufacturingPrice;
    public BigDecimal wholesalePrice;
    public BigDecimal wholesaleMarkup;
    public BigDecimal retailPrice;
    public BigDecimal retailMarkup;
    public String textCompliance;
    public String idContract;


    public UserInvoiceDetail(String number, String series, Boolean createPricing, Boolean createShipment, String sid,
                             Date date, String idItem, Boolean isWare, BigDecimal quantity, String supplier,
                             String customerWarehouse, String supplierWarehouse, BigDecimal price, BigDecimal chargePrice,
                             BigDecimal manufacturingPrice, BigDecimal wholesalePrice, BigDecimal wholesaleMarkup,
                             BigDecimal retailPrice, BigDecimal retailMarkup, String textCompliance, String idContract) {
        this.number = number;
        this.series = series;
        this.createPricing = createPricing;
        this.createShipment = createShipment;
        this.sid = sid;
        this.date = date;
        this.idItem = idItem;
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
        this.idContract = idContract;
    }
}
