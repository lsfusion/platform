package platform.server.form.entity;

public class CellEntity {

    // идентификатор (в рамках формы)
    public final int ID;

    public CellEntity(int ID) {
        this.ID = ID;
    }

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "obj" + ID;
    }

}
