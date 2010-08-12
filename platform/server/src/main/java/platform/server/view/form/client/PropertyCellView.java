package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.view.navigator.PropertyViewNavigator;

import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyCellView extends CellView implements ClientSerialize {

    public PropertyViewNavigator<?> view;

    public PropertyCellView(int ID, PropertyViewNavigator view) {
        super(ID);
        this.view = view;
    }

    public Type getType() {
        return view.view.property.getType();
    }

    public int getID() {
        return view.ID;
    }

    public String getSID() {
        return view.getSID();
    }

    public String getDefaultCaption() {
        return view.view.property.caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(view.ID);
        outStream.writeUTF(view.view.property.sID);
        outStream.writeBoolean(view.toDraw==null);
        if(view.toDraw!=null)
            outStream.writeInt(view.toDraw.ID);
    }
}
