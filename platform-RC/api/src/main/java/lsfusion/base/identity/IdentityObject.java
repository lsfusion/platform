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
    
    public void setSID(String sID) {
        this.sID = sID;
    }
}
