package lsfusion.server.form.instance;

public interface PropertyReaderInstance {

    CalcPropertyObjectInstance getPropertyObjectInstance();

    byte getTypeID();

    int getID(); // ID в рамках Type
}
