package platform.server.view.form.client;

import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.type.Type;
import platform.server.view.navigator.ObjectNavigator;

public class ClassCellView extends CellView implements ClientSerialize {

    ObjectNavigator view;
    Type type;

    public ClassCellView(int ID, ObjectNavigator view) {
        super(ID);

        this.view = view;
        this.type = new StringClass(30);
        this.show = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();
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

    String getDefaultCaption() {
        return view.caption; 
    }
}