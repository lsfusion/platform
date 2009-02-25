package platform.server.view.form.client;

import platform.server.view.form.ObjectImplement;
import platform.server.logics.classes.RemoteClass;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectImplementView extends CellView {

    public ObjectImplement view;

    public ObjectImplementView(ObjectImplement iView) {
        view = iView;
    }

    public ClassView classView = new ClassView();

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(view.ID);
        classView.serialize(outStream);
    }

    RemoteClass getBaseClass() {
        return view.baseClass;
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
