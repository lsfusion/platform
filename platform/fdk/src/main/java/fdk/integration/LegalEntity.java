package fdk.integration;

public class LegalEntity {
    public String idLegalEntity;
    public String nameLegalEntity;
    public String address;
    public String unp;
    public String okpo;
    public String phone;
    public String email;
    public String nameOwnership;
    public String shortNameOwnership;
    public String account;
    public String idChainStores;
    public String nameChainStores;
    public String idBank;
    public String country;
    public Boolean isSupplierLegalEntity;
    public Boolean isCompanyLegalEntity;
    public Boolean isCustomerLegalEntity;

    public LegalEntity(String idLegalEntity, String nameLegalEntity, String address, String unp, String okpo, String phone,
                       String email, String nameOwnership, String shortNameOwnership, String account, String idChainStores,
                       String nameChainStores, String idBank, String country, Boolean supplierLegalEntity,
                       Boolean companyLegalEntity, Boolean customerLegalEntity) {
        this.idLegalEntity = idLegalEntity;
        this.nameLegalEntity = nameLegalEntity;
        this.address = address;
        this.unp = unp;
        this.okpo = okpo;
        this.phone = phone;
        this.email = email;
        this.nameOwnership = nameOwnership;
        this.shortNameOwnership = shortNameOwnership;
        this.account = account;
        this.idChainStores = idChainStores;
        this.nameChainStores = nameChainStores;
        this.idBank = idBank;
        this.country = country;
        this.isSupplierLegalEntity = supplierLegalEntity;
        this.isCompanyLegalEntity = companyLegalEntity;
        this.isCustomerLegalEntity = customerLegalEntity;
    }

    public LegalEntity() {
    }
}
