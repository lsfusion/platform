package lsfusion.base.identity;

public class IdentityObject implements IdentityInterface {

    public int ID;
    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public IdentityObject() {
    }

    public IdentityObject(int ID) {
        this.ID = ID;
    }

    protected String sID;

    public String getSID() {
        if (sID != null)
            return sID;
        else
            return "obj" + getID();
    }
    
    public void setSID(String sID) {
        this.sID = sID;
    }
}
