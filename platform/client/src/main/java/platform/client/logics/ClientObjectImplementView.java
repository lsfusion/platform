package platform.client.logics;

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

    public ClientObjectView objectIDView;
    public ClientClassView classView;
    public ClientFunctionView addView;
    public ClientFunctionView changeClassView;
    public ClientFunctionView delView;

    public ClientObjectImplementView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientGroupObjectImplementView iGroupObject) throws ClassNotFoundException, IOException {
        objectIDView = new ClientObjectView(inStream,containers,this);
        groupObject = iGroupObject;

        ID = inStream.readInt();

        classView = new ClientClassView(inStream,containers);
        addView = new ClientFunctionView(inStream, containers);
        changeClassView = new ClientFunctionView(inStream, containers);
        delView = new ClientFunctionView(inStream, containers);
    }

    public String toString() { return objectIDView.caption; }
}
