package fdk.integration;


public class Warehouse {
    public String legalEntityID;
    public String warehouseGroupID;
    public String warehouseID;
    public String warehouseName;
    public String warehouseAddress;


    public Warehouse(String legalEntityID, String warehouseGroupID, String warehouseID, String warehouseName, String warehouseAddress) {
        this.legalEntityID = legalEntityID;
        this.warehouseGroupID = warehouseGroupID;
        this.warehouseID = warehouseID;
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
    }
}
