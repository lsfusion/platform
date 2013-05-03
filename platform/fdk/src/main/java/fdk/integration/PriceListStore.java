package fdk.integration;


public class PriceListStore {
    public String userPriceListID;
    public String item;
    public String supplier;
    public String departmentStore;
    public String currency;
    public Double price;
    public Boolean inPriceList;
    public Boolean inPriceListStock;

    public PriceListStore(String userPriceListID, String item, String supplier, String departmentStore, String currency,
                          Double price, Boolean inPriceList, Boolean inPriceListStock) {
        this.userPriceListID = userPriceListID;
        this.item = item;
        this.supplier = supplier;
        this.departmentStore = departmentStore;
        this.currency = currency;
        this.price = price;
        this.inPriceList = inPriceList;
        this.inPriceListStock = inPriceListStock;
    }
}
