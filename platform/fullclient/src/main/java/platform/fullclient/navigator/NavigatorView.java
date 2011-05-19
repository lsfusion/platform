package platform.fullclient.navigator;

import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;

import javax.swing.*;
import java.util.Set;

public abstract class NavigatorView extends JScrollPane {
    public ClientNavigatorWindow window;
    JComponent component;

    public NavigatorView(ClientNavigatorWindow window, JComponent component) {
        super(component);
        this.window = window;
        this.component = component;
    }

    public abstract void refresh(Set<ClientNavigatorElement> newElements);

    public String getCaption() {
        return window.caption;
    }

    public abstract ClientNavigatorElement getSelectedElement();

    public int getDockX() {
        return window.x;
    }

    public int getDockY() {
        return window.y;
    }

    public int getDockWidth() {
        return window.width;
    }

    public int getDockHeight() {
        return window.height;
    }
}
