package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

abstract public class ClientClass implements Serializable {

    public abstract boolean hasChildren();
    public abstract void serialize(DataOutputStream outStream) throws IOException;

    ClientClass(DataInputStream inStream) {
    }
}
