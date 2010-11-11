package tmc.integration.exp.FiscalRegistar;

import java.io.Serializable;

public class ReceiptItem implements Serializable{
    public Double price;
    public Double quantity;
    public String barCode;
    public String name;
    public Double sumPos;

    public ReceiptItem(Double price, Double quantity, String barCode, String name, Double sumPos) {
        this.price = price;
        this.quantity = quantity;
        this.barCode = barCode;
        this.name = name;
        this.sumPos = sumPos;
    }
}
