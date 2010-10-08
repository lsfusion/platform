package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientTypeSerializer {

    public static ClientType deserialize(DataInputStream inStream) throws IOException {
        if(inStream.readBoolean())
            return ClientObjectClass.type;
        else
            return (ClientType)(deserializeClientClass(inStream));
    }

    public static void serialize(DataOutputStream outStream, ClientType type) throws IOException {
        boolean objectClass = type instanceof ClientObjectType;
        outStream.writeBoolean(objectClass);
        if (!objectClass) {
            ((ClientClass)type).serialize(outStream);
        }
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream) throws IOException {
        return deserializeClientClass(inStream, false);
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream, boolean nulls) throws IOException {

        if (nulls && inStream.readBoolean()) return null;

        byte type = inStream.readByte();

        if (type == Data.OBJECT) return ClientObjectClass.deserializeObject(inStream);
        if (type == Data.INTEGER) return new ClientIntegerClass(inStream);
        if (type == Data.LONG) return new ClientLongClass(inStream);
        if (type == Data.DOUBLE) return new ClientDoubleClass(inStream);
        if (type == Data.NUMERIC) return new ClientNumericClass(inStream);
        if (type == Data.LOGICAL) return new ClientLogicalClass(inStream);
        if (type == Data.ACTION) return new ClientActionClass(inStream);
        if (type == Data.DATE) return new ClientDateClass(inStream);
        if (type == Data.STRING) return new ClientStringClass(inStream);
        if (type == Data.CLASSACTION) return new ClientClassActionClass(inStream);
        if (type == Data.FILEACTION) return new ClientFileActionClass(inStream);
        if (type == Data.IMAGE) return new ClientImageClass(inStream);
        if (type == Data.WORD) return new ClientWordClass(inStream);
        if (type == Data.EXCEL) return new ClientExcelClass(inStream);
        if (type == Data.TEXT) return new ClientTextClass(inStream);
        if (type == Data.YEAR) return new ClientIntegerClass(inStream);

        throw new IOException();
    }
}
