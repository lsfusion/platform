package fdk.integration;

public class Store extends LegalEntity {

    public String idStore;
    public String storeType;

    public Store(String idStore, String nameLegalEntity, String address, String idLegalEntity,
                 String storeType, String idChainStores) {
        this.idStore = idStore;
        this.nameLegalEntity = nameLegalEntity;
        this.address = address;
        this.idLegalEntity = idLegalEntity;
        this.storeType = storeType;
        this.idChainStores = idChainStores;
    }
}