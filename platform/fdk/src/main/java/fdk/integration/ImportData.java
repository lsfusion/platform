package fdk.integration;

import java.util.List;

public class ImportData {
    private List<Item> itemsList;
    private List<ItemGroup> itemGroupsList;
    private List<ItemGroup> parentGroupsList;
    private List<Bank> banksList;
    private List<LegalEntity> legalEntitiesList;
    private List<Employee> employeesList;
    private List<WarehouseGroup> warehouseGroupsList;
    private List<Warehouse> warehousesList;
    private List<LegalEntity> storesList;
    private List<DepartmentStore> departmentStoresList;
    private List<Contract> contractsList;
    private List<RateWaste> rateWastesList;
    private List<Ware> waresList;
    private List<PriceListStore> priceListStoresList;
    private List<PriceListSupplier> priceListSuppliersList;
    private List<UserInvoiceDetail> userInvoicesList;
    private Integer numberOfItemsAtATime;
    private Integer numberOfPriceListSuppliersAtATime;
    private Integer numberOfUserInvoicesAtATime;
    private Boolean importInactive;
    private Boolean importUserInvoicesPosted;

    public ImportData() {
    }

    public List<Item> getItemsList() {
        return itemsList;
    }

    public void setItemsList(List<Item> itemsList) {
        this.itemsList = itemsList;
    }

    public List<ItemGroup> getItemGroupsList() {
        return itemGroupsList;
    }

    public void setItemGroupsList(List<ItemGroup> itemGroupsList) {
        this.itemGroupsList = itemGroupsList;
    }

    public List<ItemGroup> getParentGroupsList() {
        return parentGroupsList;
    }

    public void setParentGroupsList(List<ItemGroup> parentGroupsList) {
        this.parentGroupsList = parentGroupsList;
    }

    public List<Bank> getBanksList() {
        return banksList;
    }

    public void setBanksList(List<Bank> banksList) {
        this.banksList = banksList;
    }

    public List<LegalEntity> getLegalEntitiesList() {
        return legalEntitiesList;
    }

    public void setLegalEntitiesList(List<LegalEntity> legalEntitiesList) {
        this.legalEntitiesList = legalEntitiesList;
    }

    public List<Employee> getEmployeesList() {
        return employeesList;
    }

    public void setEmployeesList(List<Employee> employeesList) {
        this.employeesList = employeesList;
    }

    public List<WarehouseGroup> getWarehouseGroupsList() {
        return warehouseGroupsList;
    }

    public void setWarehouseGroupsList(List<WarehouseGroup> warehouseGroupsList) {
        this.warehouseGroupsList = warehouseGroupsList;
    }

    public List<Warehouse> getWarehousesList() {
        return warehousesList;
    }

    public void setWarehousesList(List<Warehouse> warehousesList) {
        this.warehousesList = warehousesList;
    }

    public List<LegalEntity> getStoresList() {
        return storesList;
    }

    public void setStoresList(List<LegalEntity> storesList) {
        this.storesList = storesList;
    }

    public List<DepartmentStore> getDepartmentStoresList() {
        return departmentStoresList;
    }

    public void setDepartmentStoresList(List<DepartmentStore> departmentStoresList) {
        this.departmentStoresList = departmentStoresList;
    }

    public List<Contract> getContractsList() {
        return contractsList;
    }

    public void setContractsList(List<Contract> contractsList) {
        this.contractsList = contractsList;
    }

    public List<RateWaste> getRateWastesList() {
        return rateWastesList;
    }

    public void setRateWastesList(List<RateWaste> rateWastesList) {
        this.rateWastesList = rateWastesList;
    }

    public List<Ware> getWaresList() {
        return waresList;
    }

    public void setWaresList(List<Ware> waresList) {
        this.waresList = waresList;
    }

    public List<PriceListStore> getPriceListStoresList() {
        return priceListStoresList;
    }

    public void setPriceListStoresList(List<PriceListStore> priceListStoresList) {
        this.priceListStoresList = priceListStoresList;
    }

    public List<PriceListSupplier> getPriceListSuppliersList() {
        return priceListSuppliersList;
    }

    public void setPriceListSuppliersList(List<PriceListSupplier> priceListSuppliersList) {
        this.priceListSuppliersList = priceListSuppliersList;
    }

    public List<UserInvoiceDetail> getUserInvoicesList() {
        return userInvoicesList;
    }

    public void setUserInvoicesList(List<UserInvoiceDetail> userInvoicesList) {
        this.userInvoicesList = userInvoicesList;
    }

    public Integer getNumberOfItemsAtATime() {
        return numberOfItemsAtATime;
    }

    public void setNumberOfItemsAtATime(Integer numberOfItemsAtATime) {
        this.numberOfItemsAtATime = numberOfItemsAtATime;
    }

    public Integer getNumberOfPriceListSuppliersAtATime() {
        return numberOfPriceListSuppliersAtATime;
    }

    public void setNumberOfPriceListSuppliersAtATime(Integer numberOfPriceListSuppliersAtATime) {
        this.numberOfPriceListSuppliersAtATime = numberOfPriceListSuppliersAtATime;
    }

    public Integer getNumberOfUserInvoicesAtATime() {
        return numberOfUserInvoicesAtATime;
    }

    public void setNumberOfUserInvoicesAtATime(Integer numberOfUserInvoicesAtATime) {
        this.numberOfUserInvoicesAtATime = numberOfUserInvoicesAtATime;
    }

    public Boolean getImportInactive() {
        return importInactive;
    }

    public void setImportInactive(Boolean importInactive) {
        this.importInactive = importInactive;
    }

    public Boolean getImportUserInvoicesPosted() {
        return importUserInvoicesPosted==null ? false : importUserInvoicesPosted;
    }

    public void setImportUserInvoicesPosted(Boolean importUserInvoicesPosted) {
        this.importUserInvoicesPosted = importUserInvoicesPosted;
    }
}
