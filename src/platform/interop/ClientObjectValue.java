package platform.interop;

public class ClientObjectValue {

    public ClientClass cls;
    public Object object;

    public ClientObjectValue() {
    }

    public ClientObjectValue(ClientClass icls, Object iobject) {
        cls = icls;
        object = iobject;
    }
}
