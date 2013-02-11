package fdk.integration;


public class Warehouse {
    public String legalEntityID;
    public String warehouseGroupID;
    public String sid;
    public String name;
    public String address;


    public Warehouse(String legalEntityID, String warehouseGroupID, String sid, String name, String address) {
        this.legalEntityID = legalEntityID;
        this.warehouseGroupID = warehouseGroupID;
        this.sid = sid;
        this.name = name;
        this.address = address;
    }
}
