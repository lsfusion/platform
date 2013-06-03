package lsfusion.erp.integration;


import java.math.BigDecimal;
import java.util.Date;

public class UserInvoiceDetail {
    public String idUserInvoice;
    public String series;
    public String number;
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
    public String certificateText;
    public String idContract;
    public String numberDeclaration;
    public Date dateDeclaration;
    public String numberCompliance;
    public Date fromDateCompliance;
    public Date toDateCompliance;
    public Date expiryDate;
    public String bin;
    public BigDecimal rateExchange;
    public BigDecimal homePrice;
    public BigDecimal priceDuty;
    public Boolean isHomeCurrency;
    public Boolean showDeclaration;
    public Boolean showManufacturingPrice;
    public String shortNameCurrency;
    public String codeCustomsGroup;
    public BigDecimal retailVAT;


    public UserInvoiceDetail(String idUserInvoice, String series, String number, Boolean createPricing,
                             Boolean createShipment, String sid, Date date, String idItem, Boolean isWare,
                             BigDecimal quantity, String supplier, String customerWarehouse, String supplierWarehouse,
                             BigDecimal price, BigDecimal chargePrice, BigDecimal manufacturingPrice,
                             BigDecimal wholesalePrice, BigDecimal wholesaleMarkup, BigDecimal retailPrice,
                             BigDecimal retailMarkup, String certificateText, String idContract, String numberDeclaration,
                             Date dateDeclaration, String numberCompliance, Date fromDateCompliance, Date toDateCompliance,
                             Date expiryDate, String bin, BigDecimal rateExchange, BigDecimal homePrice, BigDecimal priceDuty,
                             Boolean isHomeCurrency, Boolean showDeclaration, Boolean showManufacturingPrice,
                             String shortNameCurrency, String codeCustomsGroup, BigDecimal retailVAT) {
        this.idUserInvoice = idUserInvoice;
        this.series = series;
        this.number = number;
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
        this.certificateText = certificateText;
        this.idContract = idContract;
        this.numberDeclaration = numberDeclaration;
        this.dateDeclaration = dateDeclaration;
        this.numberCompliance = numberCompliance;
        this.fromDateCompliance = fromDateCompliance;
        this.toDateCompliance = toDateCompliance;
        this.expiryDate = expiryDate;
        this.bin = bin;
        this.rateExchange = rateExchange;
        this.homePrice = homePrice;
        this.priceDuty = priceDuty;
        this.isHomeCurrency = isHomeCurrency;
        this.showDeclaration = showDeclaration;
        this.showManufacturingPrice = showManufacturingPrice;
        this.shortNameCurrency = shortNameCurrency;
        this.codeCustomsGroup = codeCustomsGroup;
        this.retailVAT = retailVAT;
    }
}
