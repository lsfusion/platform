package platform.base;

public class IdentityObject {

    private int ID;
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

    private String SID;
    
    public String getSID() {
        if (SID != null) return SID; else return "obj" + getID();
    }
    
    public void setSID(String sID) {
        this.SID = sID;
    }
}
