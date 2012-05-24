package retail.api.remote;

import java.io.Serializable;

public class ItemInfo implements Serializable {
    public String barcodeEx;
    public String name;
    public Double price;
    public Double daysExpiry;
    public Integer hoursExpiry;
    public Integer labelFormat;
    public String composition;

    public ItemInfo(String barcodeEx, String name, Double price, Double daysExpiry, Integer hoursExpiry, 
                    Integer labelFormat, String composition) {
        this.barcodeEx = barcodeEx;
        this.name = name;
        this.price = price;
        this.daysExpiry = daysExpiry;
        this.hoursExpiry = hoursExpiry;
        this.labelFormat = labelFormat;
        this.composition = composition;
    }
}
