package roman.actions.fiscaldatecs;

import java.io.Serializable;

public class ReceiptItem implements Serializable {
    public Double price;
    public Double quantity;
    public String barCode;
    public String name;
    public Double sumPos;
    public Double articleDisc;
    public Double articleDiscSum;
    public Integer taxNumber;
    public Integer artNumber;
    public Integer group;

    public ReceiptItem(Double price, Double quantity, String barCode, String name, Double sumPos,
                       Double articleDisc, Double articleDiscSum, Integer taxNumber, Integer group) {
        this.price = price;
        this.quantity = quantity;
        this.barCode = barCode;
        this.name = name;
        this.sumPos = sumPos;
        this.articleDisc = articleDisc;
        this.articleDiscSum = articleDiscSum;
        this.taxNumber = taxNumber;
        this.group = group;
    }
}
