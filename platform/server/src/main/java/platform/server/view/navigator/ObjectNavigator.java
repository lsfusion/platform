package platform.server.view.navigator;

import platform.server.data.classes.ValueClass;

public class ObjectNavigator {

    public ObjectNavigator(int iID, ValueClass iBaseClass, String iCaption) {
        ID = iID;
        caption = iCaption;
        baseClass = iBaseClass;
    }

    public GroupObjectNavigator groupTo;

    public final String caption;

    public final ValueClass baseClass;

    // идентификатор (в рамках формы)
    public final int ID;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "obj" + ID;
    }

}
