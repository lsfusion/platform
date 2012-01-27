package platform.client.form.panel.location;

import platform.client.logics.ClientPropertyDraw;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientShortcutPanelLocation extends ClientPanelLocation {
    protected ClientPropertyDraw onlyProperty;
    protected boolean defaultOne;

    public ClientShortcutPanelLocation() {}

    public ClientShortcutPanelLocation(ClientPropertyDraw onlyProperty) {
        this.onlyProperty = onlyProperty;
    }

    public boolean isShortcutLocation() {
        return true;
    }

    public boolean isToolbarLocation() {
        return false;
    }

    public void setOnlyProperty(ClientPropertyDraw property) {
        onlyProperty = property;
    }

    public ClientPropertyDraw getOnlyProperty() {
        return onlyProperty;
    }

    public void setDefault(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public boolean isDefault() {
        return defaultOne;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, onlyProperty);
        outStream.writeBoolean(defaultOne);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        onlyProperty = (ClientPropertyDraw) pool.deserializeObject(inStream);
        defaultOne = inStream.readBoolean();
    }
}
