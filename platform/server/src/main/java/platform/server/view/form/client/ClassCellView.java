package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.classes.StringClass;

public class ClassCellView extends CellView implements ClientSerialize {

    ObjectNavigator view;
    Type type;

    public ClassCellView(ObjectNavigator view) {

        this.view = view;
        this.type = new StringClass(30);
    }

    Type getType() {
        return type;
    }

    int getID() {
        return view.ID;
    }

    String getSID() {
        return view.getSID();
    }

    String getCaption() {
        return view.caption; 
    }
}