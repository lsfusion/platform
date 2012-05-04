package retail.api.remote;

import java.io.Serializable;

public class ItemInfo implements Serializable {
    public String barcodeEx;
    public String name;
    public Double price;

    public ItemInfo(String barcodeEx, String name, Double price) {
        this.barcodeEx = barcodeEx;
        this.name = name;
        this.price = price;
    }
}
