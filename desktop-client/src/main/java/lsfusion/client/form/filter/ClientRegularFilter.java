package lsfusion.client.form.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientRegularFilter extends IdentityObject implements ClientIdentitySerializable {

    public String caption = "";
    public InputEvent inputEvent;
    public Integer priority;
    public boolean showKey;

    public ClientRegularFilter() {
    }

    public ClientRegularFilter(int ID) {
        super(ID);
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showKey && inputEvent != null) {
            fullCaption += " (" + (inputEvent instanceof MouseInputEvent ? ((MouseInputEvent) inputEvent).mouseEvent : SwingUtils.getKeyStrokeCaption(((KeyInputEvent) inputEvent).keyStroke)) + ")";
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

        inputEvent = pool.readObject(inStream);
        priority = pool.readInt(inStream);
        showKey = inStream.readBoolean();
    }
}
