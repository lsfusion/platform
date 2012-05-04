package platform.client.form.panel.location;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientToolbarPanelLocation extends ClientPanelLocation {
    public boolean isShortcutLocation() {
        return false;
    }

    public boolean isToolbarLocation() {
        return true;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
    }
}
