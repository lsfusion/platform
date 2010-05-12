package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.data.type.ObjectType;
import platform.server.view.navigator.PropertyViewNavigator;
import platform.server.view.navigator.ControlViewNavigator;
import platform.server.classes.DoubleClass;
import platform.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

public class ControlCellView extends CellView implements ClientSerialize {

    public ControlViewNavigator<?,?,?,?> view;

    public ControlCellView(int ID, ControlViewNavigator view) {
        super(ID);
        this.view = view;
    }

    Type getType() {
        if(view instanceof PropertyViewNavigator)
            return ((Property)((PropertyViewNavigator)view).view.property).getType();
        else
            return ObjectType.instance; // пофигу что вернуть, у ClassCellView та же "проблема"
    }

    int getID() {
        return view.ID;
    }

    String getSID() {
        return view.getSID();
    }

    String getCaption() {
        return view.view.property.caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(view.ID);
        outStream.writeBoolean(view.toDraw==null);
        if(view.toDraw!=null)
            outStream.writeInt(view.toDraw.ID);
    }
}
