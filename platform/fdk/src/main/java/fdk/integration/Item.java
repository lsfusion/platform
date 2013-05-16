package fdk.integration;


import java.util.Date;

public class Item {
    public String idItem;
    public String itemGroupId;
    public String name;
    public String uomName;
    public String uomShortName;
    public String idUOM;
    public String brandName;
    public String idBrand;
    public String country;
    public String barcode;
    public String idBarcode;
    public Date date;
    public Boolean isWeightItem;
    public Double netWeightItem;
    public Double grossWeightItem;
    public String composition;
    public Double retailVAT;
    public String idWare;
    public Double priceWare;
    public Double wareVAT;
    public String idWriteOffRate;
    public Double baseMarkup;
    public Double retailMarkup;
    public String idBarcodePack;
    public Double amountPack;

    public Item(String idItem, String itemGroupId, String name, String uomName, String uomShortName, String idUOM,
                String brandName, String idBrand, String country, String barcode, String idBarcode, Date date,
                Boolean isWeightItem, Double netWeightItem, Double grossWeightItem, String composition,
                Double retailVAT, String idWare, Double priceWare, Double wareVAT, String idWriteOffRate,
                Double baseMarkup, Double retailMarkup, String idBarcodePack, Double amountPack) {
        this.idItem = idItem;
        this.itemGroupId = itemGroupId;
        this.name = name;
        this.uomName = uomName;
        this.uomShortName = uomShortName;
        this.idUOM = idUOM;
        this.brandName = brandName;
        this.idBrand = idBrand;
        this.country = country;
        this.barcode = barcode;
        this.idBarcode = idBarcode;
        this.date = date;
        this.isWeightItem = isWeightItem;
        this.netWeightItem = netWeightItem;
        this.grossWeightItem = grossWeightItem;
        this.composition = composition;
        this.retailVAT = retailVAT;
        this.idWare = idWare;
        this.priceWare = priceWare;
        this.wareVAT = wareVAT;
        this.idWriteOffRate = idWriteOffRate;
        this.baseMarkup = baseMarkup;
        this.retailMarkup = retailMarkup;
        this.idBarcodePack = idBarcodePack;
        this.amountPack = amountPack;
    }
}
