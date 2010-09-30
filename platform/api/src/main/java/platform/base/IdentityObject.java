package platform.base;

public class IdentityObject {

    private final int ID;
    public int getID() {
        return ID;
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
