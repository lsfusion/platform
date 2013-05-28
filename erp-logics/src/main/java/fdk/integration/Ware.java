package fdk.integration;


import java.math.BigDecimal;

public class Ware {
    public String idWare;
    public String name;
    public BigDecimal price;

    public Ware(String idWare, String name, BigDecimal price) {
        this.idWare = idWare;
        this.name = name;
        this.price = price;
    }
}
