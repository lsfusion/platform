package integration;

import java.sql.Date;

public class Reep {
    public String warehouseID;
    public String userInvoiceNumber;
    public String itemID;
    public String itemName;
    public String uomID;
    public String parentGroupID;
    public String itemGroupID;
    public String price;
    public String quantity;
    public Date date;


    public Reep(String warehouseID, String userInvoiceNumber, String itemID, String itemName, String uomID,
                String parentGroupID, String itemGroupID, String price, String quantity, Date date) {
        this.warehouseID = warehouseID;
        this.userInvoiceNumber = userInvoiceNumber;
        this.itemID = itemID;
        this.itemName = itemName;
        this.uomID = uomID;
        this.parentGroupID = parentGroupID;
        this.itemGroupID = itemGroupID;
        this.price = price;
        this.quantity = quantity;
        this.date = date;
    }
}
