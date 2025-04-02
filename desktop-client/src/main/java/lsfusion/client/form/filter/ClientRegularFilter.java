package lsfusion.client.form.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.form.event.InputBindingEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.client.base.SwingUtils.getEventCaption;

public class ClientRegularFilter extends IdentityObject implements ClientIdentitySerializable {

    public String caption = "";
    public InputBindingEvent keyInputEvent;
    public boolean showKey;
    public InputBindingEvent mouseInputEvent;
    public boolean showMouse;

    public ClientRegularFilter() {
    }

    public ClientRegularFilter(int ID) {
        super(ID);
    }

    public String getFullCaption() {
        String eventCaption = getEventCaption(keyInputEvent, showKey, mouseInputEvent, showMouse);
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
    }

    @Override
    public String toString() {
        return getFullCaption() + " (" + getID() + ")";
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);

        keyInputEvent = pool.readObject(inStream);
        showKey = inStream.readBoolean();
        mouseInputEvent = pool.readObject(inStream);
        showMouse = inStream.readBoolean();
    }
}
