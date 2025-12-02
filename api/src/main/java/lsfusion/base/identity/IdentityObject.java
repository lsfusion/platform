package lsfusion.base.identity;

public class IdentityObject implements IdentityInterface {

    public int ID;
    protected String sID;

    public IdentityObject() {
    }

    public IdentityObject(int ID) {
        this(ID, null);
    }
    
    public IdentityObject(int ID, String sID) {
        this.ID = ID;
        this.sID = sID;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getSID() {
        return sID;
    }

    public String getLogName() {
        return getSID();
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    // copy-constructor
    public IdentityObject(IdentityObject src) {
        //this.ID = src.ID;
        this.sID = src.sID;
    }
}
