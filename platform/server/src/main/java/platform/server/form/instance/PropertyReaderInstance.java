package platform.server.form.instance;

public interface PropertyReaderInstance {

    public CalcPropertyObjectInstance getPropertyObjectInstance();

    public byte getTypeID();

    public int getID(); // ID в рамках Type
}
