package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.identity.IdentityObject;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientObject extends IdentityObject implements ClientIdentitySerializable {

    public String caption;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public ClientClassChooser classChooser;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, classChooser);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupObject = pool.deserializeObject(inStream);

        caption = pool.readString(inStream);

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);

        classChooser = pool.deserializeObject(inStream);
    }

    public String getCaption(){
        return !BaseUtils.isRedundantString(caption)
                ? caption
                : !BaseUtils.isRedundantString(baseClass)
                ? baseClass.toString()
                : ClientResourceBundle.getString("logics.undefined.object");
    }

    @Override
    public String toString() {
        return !getCaption().equals(ClientResourceBundle.getString("logics.undefined.object"))
                ? getCaption() + " (" + getID() + ")"
                : getCaption();
    }
}
