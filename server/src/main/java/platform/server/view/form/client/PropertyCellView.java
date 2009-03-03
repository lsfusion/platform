package platform.server.view.form.client;

import platform.server.logics.classes.RemoteClass;
import platform.server.view.form.PropertyView;

import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyCellView extends CellView implements ClientSerialize {

    public PropertyView view;

    public PropertyCellView(PropertyView iView) {
        view = iView;
    }

    RemoteClass getBaseClass() {
        return view.view.property.getBaseClass().getCommonClass();
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
