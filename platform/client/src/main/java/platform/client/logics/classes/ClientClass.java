package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

abstract public class ClientClass implements Serializable {

    public abstract boolean hasChildren();

    ClientClass(DataInputStream inStream) {
    }

    public static ClientClass deserialize(DataInputStream inStream) throws IOException {
        return deserialize(inStream, false);
    }

    public static ClientClass deserialize(DataInputStream inStream, boolean nulls) throws IOException {

        if (nulls && inStream.readBoolean()) return null;

        byte type = inStream.readByte();

        if(type==Data.OBJECT) return ClientObjectClass.deserializeObject(inStream);
        if(type==Data.INTEGER) return new ClientIntegerClass(inStream);
        if(type==Data.LONG) return new ClientLongClass(inStream);
        if(type==Data.DOUBLE) return new ClientDoubleClass(inStream);
        if(type==Data.NUMERIC) return new ClientNumericClass(inStream);
        if(type==Data.LOGICAL) return new ClientLogicalClass(inStream);
        if(type==Data.ACTION) return new ClientActionClass(inStream);
        if(type==Data.DATE) return new ClientDateClass(inStream);
        if(type==Data.STRING) return new ClientStringClass(inStream);
        if(type==Data.CLASSACTION) return new ClientClassActionClass(inStream);

        throw new IOException();
    }
}
