package platform.server.view.navigator;

import platform.server.logics.properties.PropertyInterface;

public class PropertyViewNavigator<P extends PropertyInterface> {
    
    public PropertyObjectNavigator<P> view;

    public GroupObjectNavigator toDraw;

    public PropertyViewNavigator(int iID, PropertyObjectNavigator<P> iView, GroupObjectNavigator iToDraw) {
        view = iView;
        toDraw = iToDraw;
        ID = iID;
    }

    // идентификатор (в рамках формы)
    public int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "prop" + ID;
    }

    @Override
    public String toString() {
        return view.toString();
    }
}
