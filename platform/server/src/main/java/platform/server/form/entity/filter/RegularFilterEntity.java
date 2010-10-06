package platform.server.form.entity.filter;

import platform.base.IdentityObject;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterEntity extends IdentityObject implements ServerIdentitySerializable {
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

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        filter = (FilterEntity) pool.deserializeObject(inStream);
    }
}
