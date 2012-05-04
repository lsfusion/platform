package platform.server.form.instance;

public interface PropertyReaderInstance {

    public PropertyObjectInstance getPropertyObjectInstance();

    public byte getTypeID();

    public int getID(); // ID в рамках Type
}
