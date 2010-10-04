package platform.server.form.entity.filter;

import platform.base.IdentityObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterEntity extends IdentityObject implements IdentitySerializable {
    public transient FilterEntity filter;
    public String name = "";
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
