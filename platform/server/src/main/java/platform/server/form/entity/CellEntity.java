package platform.server.form.entity;

public class CellEntity {

    private final int ID;
    public int getID() {
        return ID;
    }

    public CellEntity(int ID) {
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
