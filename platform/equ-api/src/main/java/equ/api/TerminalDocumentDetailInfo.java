package equ.api;

import java.io.Serializable;

public class TerminalDocumentDetailInfo implements Serializable {
    public Integer id;
    public String barcode;
    public String name;
    public Boolean isNew;
    public Double quantity;
    public Double price;
    public Double sum;    

    public TerminalDocumentDetailInfo(Integer id, String barcode, String name, Boolean isNew, 
                                      Double quantity, Double price, Double sum) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.isNew = isNew;
        this.quantity = quantity;
        this.price = price;
        this.sum = sum;
    }
}
