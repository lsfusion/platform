package lsfusion.client.form.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.identity.IdentityObject;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientClass;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientObject extends IdentityObject implements ClientIdentitySerializable {

    public String caption;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupObject = pool.deserializeObject(inStream);

        caption = pool.readString(inStream);

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);
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
