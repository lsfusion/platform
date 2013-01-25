package fdk.integration;


public class Assortment {
    public String item;
    public String supplier;
    public String userPriceList;
    public String departmentStore;
    public String currency;
    public Double price;
    public Boolean inPriceList;

    public Assortment(String item, String supplier, String userPriceList, String departmentStore, String currency,
                      Double price, Boolean inPriceList) {
        this.item = item;
        this.supplier = supplier;
        this.userPriceList = userPriceList;
        this.departmentStore = departmentStore;
        this.currency = currency;
        this.price = price;
        this.inPriceList = inPriceList;
    }
}
