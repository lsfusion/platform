package platform.interop;

public class ClientChangeObjectValue extends ClientChangeValue {
    Object value;

    public ClientChangeObjectValue(ClientClass icls, Object ivalue) {
        super(icls);
        value = ivalue;
    }

    public ClientObjectValue getObjectValue(Object ivalue) {
        return new ClientObjectValue(cls, value);
    }
}
