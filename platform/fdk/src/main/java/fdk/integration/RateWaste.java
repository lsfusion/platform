package fdk.integration;


import java.math.BigDecimal;

public class RateWaste {
    public String idRateWaste;
    public String name;
    public BigDecimal coef;
    public String country;

    public RateWaste(String idRateWaste, String name, BigDecimal coef, String country) {
        this.idRateWaste = idRateWaste;
        this.name = name;
        this.coef = coef;
        this.country = country;
    }
}
