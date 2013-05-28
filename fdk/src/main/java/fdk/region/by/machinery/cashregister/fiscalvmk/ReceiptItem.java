package fdk.region.by.machinery.cashregister.fiscalvmk;

import java.io.Serializable;

public class ReceiptItem implements Serializable {
    public Long price;
    public Double quantity;
    public String barCode;
    public String name;
    public Long sumPos;
    public Double articleDisc;
    public Double articleDiscSum;
    public Integer taxNumber;
    public Integer group;

    public ReceiptItem(Long price, Double quantity, String barCode, String name, Long sumPos,
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
