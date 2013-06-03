package lsfusion.client.logics.classes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

abstract public class ClientClass implements Serializable {

    public abstract boolean hasChildren();
    public abstract void serialize(DataOutputStream outStream) throws IOException;

    public abstract String getSID();

    public abstract String getCode();
    
    public abstract ClientType getType();

    protected ClientClass() {
    }

    public static ClientTypeClass[] getEnumTypeClasses() {
        return new ClientTypeClass[] {
            ClientStringClass.getTypeClass(false, false),
            ClientStringClass.getTypeClass(true, false),
            ClientStringClass.getTypeClass(false, true),
            ClientStringClass.getTypeClass(true, true),
            ClientObjectClass.type,
            ClientNumericClass.type,
            ClientDateClass.instance,
            ClientLogicalClass.instance,
            ClientIntegerClass.instance,
            ClientDoubleClass.instance,
            ClientLongClass.instance,
            ClientWordClass.instance,
            ClientImageClass.instance,
            ClientExcelClass.instance,
            ClientPDFClass.instance,
            ClientDynamicFormatFileClass.instance,
            ClientTimeClass.instance
        };
    }
}
