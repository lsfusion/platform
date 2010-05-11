package platform.client.logics;

import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class ClientObjectImplementView implements Serializable {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
    }

    // вручную заполняется
    public ClientGroupObjectImplementView groupObject;

    public ClientObjectCellView objectCellView;

    public ClientClass baseClass;

    public ClientClassCellView classCellView;

    public ClientClassView classView;
    public ClientFunctionView addView;
    public ClientFunctionView changeClassView;
    public ClientFunctionView delView;

    public ClientObjectImplementView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientGroupObjectImplementView iGroupObject) throws ClassNotFoundException, IOException {

        groupObject = iGroupObject;

        ID = inStream.readInt();

        baseClass = ClientClass.deserialize(inStream);

        objectCellView = new ClientObjectCellView(inStream, containers, this);
        classCellView = new ClientClassCellView(inStream, containers, this);
        classView = new ClientClassView(inStream,containers);
        addView = new ClientFunctionView(inStream, containers);
        changeClassView = new ClientFunctionView(inStream, containers);
        delView = new ClientFunctionView(inStream, containers);
    }

    public String toString() { return objectCellView.caption; }
}
