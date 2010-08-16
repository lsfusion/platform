package platform.server.form.instance;

public abstract class CellInstance {

    // идентификатор (в рамках формы)
    public final int ID;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public final String sID;

    public CellInstance(int ID, String sID) {
        this.ID = ID;
        this.sID = sID;
    }
}
