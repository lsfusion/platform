package platform.interop;

abstract public class ClientChangeValue {
    ClientClass cls;

    ClientChangeValue(ClientClass icls) {
        cls = icls;
    }

    abstract public ClientObjectValue getObjectValue(Object value);
}
