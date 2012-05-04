package platform.client.form.panel.location;

import platform.client.serialization.ClientCustomSerializable;

public abstract class ClientPanelLocation implements ClientCustomSerializable {
    public abstract boolean isShortcutLocation();
    public abstract boolean isToolbarLocation();
}
