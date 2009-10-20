package platform.server.view.form.client;

import platform.server.data.types.Type;
import platform.server.view.navigator.ObjectNavigator;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectImplementView extends CellView {

    public ObjectNavigator view;

    public ObjectImplementView(ObjectNavigator iView) {
        view = iView;
    }

    public ClassView classView = new ClassView();

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(view.show);

        outStream.writeInt(view.ID);
        classView.serialize(outStream);
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

    String getCaption() {
        return view.caption;
    }
}
