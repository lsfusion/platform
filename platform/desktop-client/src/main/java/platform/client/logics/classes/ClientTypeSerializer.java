package platform.client.logics.classes;

import platform.interop.Data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientTypeSerializer {

    public static ClientType deserializeClientType(byte[] typeData) throws IOException {
        return deserializeClientType(new DataInputStream(new ByteArrayInputStream(typeData)));
    }

    public static ClientType deserializeClientType(DataInputStream inStream) throws IOException {
        if (inStream.readBoolean()) {
            return ClientObjectClass.type;
        } else {
            return (ClientType) (deserializeClientClass(inStream));
        }
    }

    public static void serializeClientType(DataOutputStream outStream, ClientType type) throws IOException {
        boolean objectClass = type instanceof ClientObjectType;
        outStream.writeBoolean(objectClass);
        if (!objectClass) {
            ((ClientClass) type).serialize(outStream);
        }
    }

    public static ClientObjectClass deserializeClientObjectClass(DataInputStream inStream) throws IOException {
        return (ClientObjectClass) deserializeClientClass(inStream);
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream) throws IOException {
        return deserializeClientClass(inStream, false);
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream, boolean nulls) throws IOException {

        if (nulls && inStream.readBoolean()) return null;

        byte type = inStream.readByte();

        if (type == Data.OBJECT) return ClientObjectClass.deserialize(inStream);
        if (type == Data.INTEGER) return ClientIntegerClass.instance;
        if (type == Data.LONG) return ClientLongClass.instance;
        if (type == Data.DOUBLE) return ClientDoubleClass.instance;
        if (type == Data.NUMERIC) return new ClientNumericClass(inStream);
        if (type == Data.LOGICAL) return ClientLogicalClass.instance;
        if (type == Data.DATE) return ClientDateClass.instance;
        if (type == Data.STRING) return new ClientStringClass(inStream);
        if (type == Data.INSENSITIVESTRING) return new ClientInsensitiveStringClass(inStream);
        if (type == Data.TEXT) return ClientTextClass.instance;
        if (type == Data.YEAR) return ClientIntegerClass.instance;
        if (type == Data.DATETIME) return ClientDateTimeClass.instance;
        if (type == Data.TIME) return ClientTimeClass.instance;
        if (type == Data.COLOR) return ClientColorClass.instance;

        if (type == Data.PDF) return new ClientPDFClass(inStream);
        if (type == Data.IMAGE) return new ClientImageClass(inStream);
        if (type == Data.WORD) return new ClientWordClass(inStream);
        if (type == Data.EXCEL) return new ClientExcelClass(inStream);
        if (type == Data.DYNAMICFORMATFILE) return new ClientDynamicFormatFileClass(inStream);
        if (type == Data.CUSTOMSTATICFORMATFILE) return new ClientCustomStaticFormatFileClass(inStream);

        if (type == Data.ACTION) return new ClientActionClass(inStream);

        throw new IOException();
    }
}
