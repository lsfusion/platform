package lsfusion.erp.integration;


import java.math.BigDecimal;
import java.util.Date;

public class Item {
    public String idItem;
    public String itemGroupId;
    public String nameItem;
    public String nameUOM;
    public String shortNameUOM;
    public String idUOM;
    public String brandName;
    public String idBrand;
    public String country;
    public String barcode;
    public String idBarcode;
    public Date date;
    public Boolean isWeightItem;
    public BigDecimal netWeightItem;
    public BigDecimal grossWeightItem;
    public String compositionItem;
    public BigDecimal retailVAT;
    public String idWare;
    public BigDecimal priceWare;
    public BigDecimal wareVAT;
    public String idWriteOffRate;
    public BigDecimal baseMarkup;
    public BigDecimal retailMarkup;
    public String idBarcodePack;
    public BigDecimal amountPack;
    public String idManufacturer;
    public String nameManufacturer;
    public String codeCustomsGroup;
    public String codeCustomsZone;

    public Item(String idItem, String itemGroupId, String nameItem, String nameUOM, String shortNameUOM, String idUOM,
                String brandName, String idBrand, String country, String barcode, String idBarcode, Date date,
                Boolean isWeightItem, BigDecimal netWeightItem, BigDecimal grossWeightItem, String compositionItem,
                BigDecimal retailVAT, String idWare, BigDecimal priceWare, BigDecimal wareVAT, String idWriteOffRate,
                BigDecimal baseMarkup, BigDecimal retailMarkup, String idBarcodePack, BigDecimal amountPack,
                String idManufacturer, String nameManufacturer, String codeCustomsGroup, String codeCustomsZone) {
        this.idItem = idItem;
        this.itemGroupId = itemGroupId;
        this.nameItem = nameItem;
        this.nameUOM = nameUOM;
        this.shortNameUOM = shortNameUOM;
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
        this.compositionItem = compositionItem;
        this.retailVAT = retailVAT;
        this.idWare = idWare;
        this.priceWare = priceWare;
        this.wareVAT = wareVAT;
        this.idWriteOffRate = idWriteOffRate;
        this.baseMarkup = baseMarkup;
        this.retailMarkup = retailMarkup;
        this.idBarcodePack = idBarcodePack;
        this.amountPack = amountPack;
        this.idManufacturer = idManufacturer;
        this.nameManufacturer = nameManufacturer;
        this.codeCustomsGroup = codeCustomsGroup;
        this.codeCustomsZone = codeCustomsZone;
    }
}
