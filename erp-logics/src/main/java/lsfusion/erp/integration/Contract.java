package lsfusion.erp.integration;


import java.util.Date;

public class Contract {
    public String idContract;
    public String idSupplier;
    public String idCustomer;
    public String number;
    public Date dateFrom;
    public Date dateTo;
    public String currency;


    public Contract(String idContract, String idSupplier, String idCustomer, String number, Date dateFrom, Date dateTo,
                    String currency) {
        this.idContract = idContract;
        this.idSupplier = idSupplier;
        this.idCustomer = idCustomer;
        this.number = number;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.currency = currency;
    }
}
