package lsfusion.client.form.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class ClientRegularFilter extends IdentityObject implements ClientIdentitySerializable {

    public String caption = "";
    public KeyInputEvent keyEvent;
    public Integer priority;
    public boolean showKey;

    public ClientRegularFilter() {
    }

    public ClientRegularFilter(int ID) {
        super(ID);
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showKey && keyEvent != null) {
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(keyEvent.keyStroke) + ")";
        }
        return fullCaption;
    }

    @Override
    public String toString() {
        return getFullCaption() + " (" + getID() + ")";
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);

        keyEvent = pool.readObject(inStream);
        if(keyEvent != null) {
            if(keyEvent.bindingModes == null)
                keyEvent.bindingModes = new HashMap<>();
            if(keyEvent.bindingModes.get("preview") == null) {
                keyEvent.bindingModes.put("preview", BindingMode.NO);
            }
        }
        priority = pool.readInt(inStream);
        showKey = inStream.readBoolean();
    }
}
