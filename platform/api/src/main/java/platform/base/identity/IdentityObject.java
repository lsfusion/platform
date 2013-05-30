package platform.base.identity;

import platform.base.col.interfaces.mutable.MOrderExclSet;

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
