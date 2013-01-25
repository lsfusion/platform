package fdk.integration;


public class Bank {
    public String bankID;
    public String name;
    public String address;
    public String department;
    public String mfo;
    public String cbu;

    public Bank(String bankID, String name, String address, String department, String mfo, String cbu) {
        this.bankID = bankID;
        this.name = name;
        this.address = address;
        this.department = department;
        this.mfo = mfo;
        this.cbu = cbu;
    }
}
