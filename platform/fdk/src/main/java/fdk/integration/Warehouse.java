package fdk.integration;


public class Warehouse {
    public String idLegalEntity;
    public String idWarehouseGroup;
    public String idWarehouse;
    public String warehouseName;
    public String warehouseAddress;


    public Warehouse(String idLegalEntity, String idWarehouseGroup, String idWarehouse, String warehouseName, String warehouseAddress) {
        this.idLegalEntity = idLegalEntity;
        this.idWarehouseGroup = idWarehouseGroup;
        this.idWarehouse = idWarehouse;
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
    }
}
