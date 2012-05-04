package platform.server.form.view.panellocation;

import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ToolbarPanelLocationView extends PanelLocationView {
    public boolean isShortcutLocation() {
        return false;
    }

    public boolean isToolbarLocation() {
        return true;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
    }
}
