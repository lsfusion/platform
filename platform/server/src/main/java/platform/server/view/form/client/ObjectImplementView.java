package platform.server.view.form.client;

import platform.server.view.navigator.ObjectNavigator;
import platform.base.IDGenerator;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectImplementView implements ClientSerialize {

    public ObjectNavigator view;

    public ObjectCellView objectCellView;
    public ClassCellView classCellView;
    public ClassView classView;
    public FunctionView addView;
    public FunctionView changeClassView;
    public FunctionView delView;

    public ObjectImplementView(IDGenerator idGen, ObjectNavigator iView) {

        view = iView;

        objectCellView = new ObjectCellView(idGen.idShift(), view);
        classCellView = new ClassCellView(idGen.idShift(), view);
        classView = new ClassView(idGen.idShift());
        addView = new FunctionView(idGen.idShift());
        changeClassView = new FunctionView(idGen.idShift());
        delView = new FunctionView(idGen.idShift());
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(view.ID);

        view.baseClass.serialize(outStream);

        objectCellView.serialize(outStream);
        outStream.writeBoolean(view.show);

        classCellView.serialize(outStream);
        outStream.writeBoolean(view.showClass);

        classView.serialize(outStream);
        outStream.writeBoolean(view.showTree);        

        addView.serialize(outStream);
        changeClassView.serialize(outStream);
        delView.serialize(outStream);
    }
}
