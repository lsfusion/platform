package tmc.integration.exp.FiscalRegister;

import java.io.Serializable;

public class ObligationItem implements Serializable {
    public String name;
    public String barcode;
    public Double sum;

    public ObligationItem(String name, String barcode, Double sum) {
        this.name = name;
        this.barcode = barcode;
        this.sum = sum;
    }
}
