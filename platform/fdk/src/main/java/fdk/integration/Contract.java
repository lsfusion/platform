package fdk.integration;


import java.util.Date;

public class Contract {
    public String contractID;
    public String supplierID;
    public String customerID;
    public String number;
    public Date dateFrom;
    public Date dateTo;


    public Contract(String contractID, String supplierID, String customerID, String number, Date dateFrom, Date dateTo) {
        this.contractID = contractID;
        this.supplierID = supplierID;
        this.customerID = customerID;
        this.number = number;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }
}
