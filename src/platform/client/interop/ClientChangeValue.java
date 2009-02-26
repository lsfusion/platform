package platform.client.interop;

import platform.client.interop.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;

abstract public class ClientChangeValue {
    ClientClass cls;

    ClientChangeValue(DataInputStream inStream) throws IOException {
        cls = ClientClass.deserialize(inStream);
    }

    abstract public ClientObjectValue getObjectValue(Object value);

    public static ClientChangeValue deserialize(DataInputStream inStream) throws IOException {

        if(inStream.readBoolean()) return null;

        int classType = inStream.readByte();

        if(classType == 0) return new ClientChangeObjectValue(inStream);
        if(classType == 1) return new ClientChangeCoeffValue(inStream);

        throw new IOException();
    }
}
