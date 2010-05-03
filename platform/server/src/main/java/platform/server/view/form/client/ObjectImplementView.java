package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.view.navigator.ObjectNavigator;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectImplementView extends CellView {

    public ObjectNavigator view;

    public ObjectImplementView(ObjectNavigator iView) {
        view = iView;

        classCellView = new ClassCellView(view);
    }

    public ClassCellView classCellView;
    public ClassView classView = new ClassView();
    public FunctionView addView = new FunctionView();
    public FunctionView changeClassView = new FunctionView();
    public FunctionView delView = new FunctionView();

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(view.show);

        outStream.writeInt(view.ID);

        view.baseClass.serialize(outStream);

        classCellView.serialize(outStream);
        outStream.writeBoolean(view.showClass);

        classView.serialize(outStream);
        outStream.writeBoolean(view.showTree);        

        addView.serialize(outStream);
        changeClassView.serialize(outStream);
        delView.serialize(outStream);
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
