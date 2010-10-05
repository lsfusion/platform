package platform.interop.serialization;

public interface IdentitySerializable<P extends SerializationPool> extends CustomSerializable<P> {
    public int getID();
}
