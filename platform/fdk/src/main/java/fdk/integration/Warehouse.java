package fdk.integration;


public class Warehouse {
    public String legalEntityID;
    public String sid;
    public String name;
    public String address;


    public Warehouse(String legalEntityID, String sid, String name, String address) {
        this.legalEntityID = legalEntityID;
        this.sid = sid;
        this.name = name;
        this.address = address;
    }
}
