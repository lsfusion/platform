package fdk.integration;


public class UOM {
    public String uomName;
    public String uomShortName;
    public Double netWeight;
    public Double grossWeight;

    public UOM(String uomName, String uomShortName, Double netWeight, Double grossWeight) {
        this.uomName = uomName;
        this.uomShortName = uomShortName;
        this.netWeight = netWeight;
        this.grossWeight = grossWeight;
    }
}
