package fdk.integration;


import java.math.BigDecimal;

public class UOM {
    public String uomFullName;
    public String uomName;
    public String uomShortName;
    public BigDecimal netWeight;
    public BigDecimal grossWeight;

    public UOM(String uomFullName, String uomName, String uomShortName, BigDecimal netWeight, BigDecimal grossWeight) {
        this.uomFullName = uomFullName;
        this.uomName = uomName;
        this.uomShortName = uomShortName;
        this.netWeight = netWeight;
        this.grossWeight = grossWeight;
    }
}
