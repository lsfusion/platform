package fdk.integration;

public class Store extends LegalEntity {

    public String storeID;
    public String storeType;

    public Store(String storeID, String nameLegalEntity, String address, String legalEntityID,
                 String storeType, String chainStoresID) {
        this.storeID = storeID;
        this.nameLegalEntity = nameLegalEntity;
        this.address = address;
        this.legalEntityID = legalEntityID;
        this.storeType = storeType;
        this.chainStoresID = chainStoresID;
    }
}