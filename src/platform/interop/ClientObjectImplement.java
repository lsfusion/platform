package platform.interop;

import java.io.Serializable;

public class ClientObjectImplement implements Serializable {

    public Integer ID = 0;

    public ClientGroupObjectImplement groupObject;

    public String caption = "";

    public ClientObjectView objectIDView = new ClientObjectView();
    public ClientClassView classView = new ClientClassView();

    public ClientObjectImplement() {
    }

    public String toString() { return caption; }
}
