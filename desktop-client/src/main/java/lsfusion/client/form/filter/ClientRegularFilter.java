package lsfusion.client.form.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.client.base.SwingUtils.getEventCaption;

public class ClientRegularFilter extends IdentityObject implements ClientIdentitySerializable {

    public String caption = "";
    public KeyInputEvent keyInputEvent;
    public Integer keyPriority;
    public boolean showKey;
    public MouseInputEvent mouseInputEvent;
    public Integer mousePriority;
    public boolean showMouse;

    public ClientRegularFilter() {
    }

    public ClientRegularFilter(int ID) {
        super(ID);
    }

    public String getFullCaption() {
        String eventCaption = getEventCaption(showKey && keyInputEvent != null ? SwingUtils.getKeyStrokeCaption(keyInputEvent.keyStroke) : null,
                showMouse && mouseInputEvent != null ? mouseInputEvent.mouseEvent : null);
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
        keyPriority = pool.readInt(inStream);
        showKey = inStream.readBoolean();
        mouseInputEvent = pool.readObject(inStream);
        mousePriority = pool.readInt(inStream);
        showMouse = inStream.readBoolean();
    }
}
