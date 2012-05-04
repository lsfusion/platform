package platform.server.form.view.panellocation;

import platform.server.form.view.PropertyDrawView;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ShortcutPanelLocationView extends PanelLocationView {
    protected PropertyDrawView onlyProperty;
    protected boolean defaultOne;

    public ShortcutPanelLocationView() {}

    public ShortcutPanelLocationView(PropertyDrawView onlyProperty) {
        this.onlyProperty = onlyProperty;
    }

    public ShortcutPanelLocationView(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public ShortcutPanelLocationView(PropertyDrawView onlyProperty, boolean defaultOne) {
        this.onlyProperty = onlyProperty;
        this.defaultOne = defaultOne;
    }

    public boolean isShortcutLocation() {
        return true;
    }

    public boolean isToolbarLocation() {
        return false;
    }

    public void setOnlyProperty(PropertyDrawView property) {
        onlyProperty = property;
    }

    public PropertyDrawView getOnlyProperty() {
        return onlyProperty;
    }

    public void setDefault(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public boolean isDefault() {
        return defaultOne;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, onlyProperty);
        outStream.writeBoolean(defaultOne);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        onlyProperty = (PropertyDrawView) pool.deserializeObject(inStream);
        defaultOne = inStream.readBoolean();
    }
}
