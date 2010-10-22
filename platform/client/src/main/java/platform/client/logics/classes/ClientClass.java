package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

abstract public class ClientClass implements Serializable {

    public abstract boolean hasChildren();
    public abstract void serialize(DataOutputStream outStream) throws IOException;

    public abstract ClientType getType();

    protected ClientClass() {
    }

    ClientClass(DataInputStream inStream) {
    }

    public static ClientTypeClass[] getEnumTypeClasses() {
        return new ClientTypeClass[] {
            ClientStringClass.type,
            ClientObjectClass.type,
            ClientNumericClass.type,
            ClientDateClass.instance,
            ClientTextClass.instance,
            ClientLogicalClass.instance,
            ClientIntegerClass.instance,
            ClientDoubleClass.instance,
            ClientLongClass.instance,
            ClientWordClass.instance,
            ClientImageClass.instance,
            ClientExcelClass.instance
        };
    }
}
