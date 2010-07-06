package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.view.navigator.ObjectNavigator;

public class ObjectCellView extends CellView {

    ObjectNavigator view;
    
    public ObjectCellView(int ID, ObjectNavigator view) {
        super(ID);

        this.view = view;
    }

    Type getType() {
        return view.baseClass.getType();
    }

    int getID() {
        return view.ID;
    }

    String getSID() {
        return view.getSID();
    }

    String getDefaultCaption() {
        return view.caption;
    }

}
