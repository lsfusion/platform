package platform.server.view.form;

public abstract class CellView {

    // идентификатор (в рамках формы)
    public final int ID;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public final String sID;

    public CellView(int ID, String sID) {
        this.ID = ID;
        this.sID = sID;
    }
}
