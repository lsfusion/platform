package platform.server.logics.panellocation;

import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;

public class ShortcutPanelLocation extends PanelLocation {
    protected Property onlyProperty;
    protected boolean defaultOne;

    public ShortcutPanelLocation() {}

    public ShortcutPanelLocation(Property onlyProperty) {
        this.onlyProperty = onlyProperty;
    }

    public ShortcutPanelLocation(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public ShortcutPanelLocation(Property onlyProperty, boolean defaultOne) {
        this.onlyProperty = onlyProperty;
        this.defaultOne = defaultOne;
    }

    public boolean isShortcutLocation() {
        return true;
    }

    public boolean isToolbarLocation() {
        return false;
    }

    public void setOnlyProperty(Property property) {
        onlyProperty = property;
    }

    public void setOnlyProperty(LP property) {
        setOnlyProperty(property.property);
    }

    public Property getOnlyProperty() {
        return onlyProperty;
    }

    public void setDefault(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public boolean isDefault() {
        return defaultOne;
    }
}