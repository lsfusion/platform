package lsfusion.erp.integration;


public class Bank {
    public String idBank;
    public String name;
    public String address;
    public String department;
    public String mfo;
    public String cbu;

    public Bank(String idBank, String name, String address, String department, String mfo, String cbu) {
        this.idBank = idBank;
        this.name = name;
        this.address = address;
        this.department = department;
        this.mfo = mfo;
        this.cbu = cbu;
    }
}
