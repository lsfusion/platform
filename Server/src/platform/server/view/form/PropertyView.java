package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;

// представление св-ва
public class PropertyView<P extends PropertyInterface> {
    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public PropertyView(int iID, PropertyObjectImplement<P> iView, GroupObjectImplement iToDraw) {
        view = iView;
        toDraw = iToDraw;
        ID = iID;
    }

    public PropertyView(PropertyView<P> navigatorProperty) {

        ID = navigatorProperty.ID;
        view = navigatorProperty.view;
        toDraw = navigatorProperty.toDraw;
    }

    public String toString() {
        return view.toString();
    }

    // идентификатор (в рамках формы)
    public int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "prop" + ID;
    }
}
