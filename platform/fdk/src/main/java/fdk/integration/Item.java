package fdk.integration;


import java.util.Date;

public class Item {
    public String itemID;
    public String k_grtov;
    public String name;
    public String uomName;
    public String uomShortName;
    public String uomID;
    public String brandName;
    public String brandID;
    public String country;
    public String barcode;
    public Date date;
    public Boolean isWeightItem;
    public Double netWeightItem;
    public Double grossWeightItem;
    public String composition;
    public Double retailVAT;
    public String wareID;
    public Double priceWare;
    public Double ndsWareField;
    public String writeOffRateID;
    public Double baseMarkup;
    public Double retailMarkup;

    public Item(String itemID, String k_grtov, String name, String uomName, String uomShortName, String uomID,
                String brandName, String brandID, String country, String barcode, Date date, Boolean isWeightItem,
                Double netWeightItem, Double grossWeightItem, String composition, Double retailVAT, String wareID,
                Double priceWare, Double ndsWareField, String writeOffRateID, Double baseMarkup,  Double retailMarkup) {
        this.itemID = itemID;
        this.k_grtov = k_grtov;
        this.name = name;
        this.uomName = uomName;
        this.uomShortName = uomShortName;
        this.uomID = uomID;
        this.brandName = brandName;
        this.brandID = brandID;
        this.country = country;
        this.barcode = barcode;
        this.date = date;
        this.isWeightItem = isWeightItem;
        this.netWeightItem = netWeightItem;
        this.grossWeightItem = grossWeightItem;
        this.composition = composition;
        this.retailVAT = retailVAT;
        this.wareID = wareID;
        this.priceWare = priceWare;
        this.ndsWareField = ndsWareField;
        this.writeOffRateID = writeOffRateID;
        this.baseMarkup = baseMarkup;
        this.retailMarkup = retailMarkup;
    }
}
