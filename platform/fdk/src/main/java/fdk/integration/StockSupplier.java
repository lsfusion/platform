package fdk.integration;

public class StockSupplier {
    public String userPriceListID;
    public String departmentStore;
    public Boolean inPriceList;
    public Boolean inPriceListStock;

    public StockSupplier(String userPriceListID, String departmentStore, Boolean inPriceList, Boolean inPriceListStock) {
        this.userPriceListID = userPriceListID;
        this.departmentStore = departmentStore;
        this.inPriceList = inPriceList;
        this.inPriceListStock = inPriceListStock;
    }
}
