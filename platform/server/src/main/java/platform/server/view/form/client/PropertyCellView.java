package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.view.navigator.PropertyViewNavigator;

import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyCellView extends CellView implements ClientSerialize {

    public PropertyViewNavigator view;

    public PropertyCellView(int ID, PropertyViewNavigator iView) {
        super(ID);
        view = iView;
    }

    Type getType() {
        return view.view.property.getType();
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
