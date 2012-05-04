package platform.server.form.entity.filter;

import platform.base.identity.IdentityObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class RegularFilterEntity extends IdentityObject implements ServerIdentitySerializable {
    public transient FilterEntity filter;
    public String name = "";
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterEntity() {
        
    }
    
    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname) {
        this(iID, ifilter, iname, null);
    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        filter = (FilterEntity) pool.deserializeObject(inStream);

        name = inStream.readUTF();
        try {
            key = (KeyStroke) new ObjectInputStream(inStream).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(ServerResourceBundle.getString("form.entity.filter.can.not.deserialize.regular.filter.entity"));
        }

        showKey = inStream.readBoolean();
    }
}
