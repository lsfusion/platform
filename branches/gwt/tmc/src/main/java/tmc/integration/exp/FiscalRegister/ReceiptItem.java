package tmc.integration.exp.FiscalRegister;

import java.io.Serializable;

public class ReceiptItem implements Serializable {
    public Double price;
    public Double quantity;
    public String barCode;
    public String name;
    public Double sumPos;
    public Double articleDisc;
    public Number articleDiscSum;

    public ReceiptItem(Double price, Double quantity, String barCode, String name, Double sumPos, Double articleDisc, Number articleDiscSum) {
        this.price = price;
        this.quantity = quantity;
        this.barCode = barCode;
        this.name = name;
        this.sumPos = sumPos;
        this.articleDisc = articleDisc;
        this.articleDiscSum = articleDiscSum;
    }
}
