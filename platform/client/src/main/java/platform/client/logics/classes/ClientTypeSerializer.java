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
        if (type == Data.INTEGER) return ClientIntegerClass.instance;
        if (type == Data.LONG) return ClientLongClass.instance;
        if (type == Data.DOUBLE) return ClientDoubleClass.instance;
        if (type == Data.NUMERIC) return new ClientNumericClass(inStream);
        if (type == Data.LOGICAL) return ClientLogicalClass.instance;
        if (type == Data.DATE) return ClientDateClass.instance;
        if (type == Data.STRING) return new ClientStringClass(inStream);
        if (type == Data.INSENSITIVESTRING) return new ClientInsensitiveStringClass(inStream);
        if (type == Data.IMAGE) return ClientImageClass.instance;
        if (type == Data.WORD) return ClientWordClass.instance;
        if (type == Data.EXCEL) return ClientExcelClass.instance;
        if (type == Data.TEXT) return ClientTextClass.instance;
        if (type == Data.YEAR) return ClientIntegerClass.instance;
        if (type == Data.PDF) return ClientPDFClass.instance;

        if (type == Data.ACTION) return new ClientActionClass(inStream);
        if (type == Data.CLASSACTION) return new ClientClassActionClass(inStream);
        if (type == Data.FILEACTION) return new ClientFileActionClass(inStream);
        
        throw new IOException();
    }
}
