package fdk.integration;


import java.math.BigDecimal;

public class PriceListStore {
    public String idUserPriceList;
    public String item;
    public String supplier;
    public String departmentStore;
    public String currency;
    public BigDecimal price;
    public Boolean inPriceList;
    public Boolean inPriceListStock;

    public PriceListStore(String idUserPriceList, String item, String supplier, String departmentStore, String currency,
                          BigDecimal price, Boolean inPriceList, Boolean inPriceListStock) {
        this.idUserPriceList = idUserPriceList;
        this.item = item;
        this.supplier = supplier;
        this.departmentStore = departmentStore;
        this.currency = currency;
        this.price = price;
        this.inPriceList = inPriceList;
        this.inPriceListStock = inPriceListStock;
    }
}
