package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class ClientObjectImplementView implements Serializable {

    public Integer ID = 0;

    // вручную заполняется
    public ClientGroupObjectImplementView groupObject;

    public ClientObjectView objectIDView;
    public ClientClassView classView;

    public ClientObjectImplementView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientGroupObjectImplementView iGroupObject) throws ClassNotFoundException, IOException {
        objectIDView = new ClientObjectView(inStream,containers,this);
        groupObject = iGroupObject;

        ID = inStream.readInt();

        classView = new ClientClassView(inStream,containers);
    }

    public String toString() { return objectIDView.caption; }
}
