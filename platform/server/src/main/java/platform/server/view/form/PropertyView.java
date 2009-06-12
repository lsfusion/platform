package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;

// представление св-ва
public class PropertyView<P extends PropertyInterface> {
    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public PropertyView(int iID, String iSID, PropertyObjectImplement<P> iView, GroupObjectImplement iToDraw) {
        view = iView;
        toDraw = iToDraw;
        ID = iID;
        sID = iSID;
    }

    public String toString() {
        return view.toString();
    }

    // идентификатор (в рамках формы)
    public int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        return sID;
    }
}
