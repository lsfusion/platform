package fdk.integration;


import java.util.Date;

public class Price {
    public String item;
    public String departmentStore;
    public Date date;
    public Double price;
    public Double markup;

    public Price(String item, String departmentStore, Date date, Double price, Double markup) {
        this.item = item;
        this.departmentStore = departmentStore;
        this.date = date;
        this.price = price;
        this.markup = markup;
    }
}
