package platform.client.navigator;

import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;

import javax.swing.*;
import java.util.Set;

public abstract class NavigatorView extends JScrollPane {
    public ClientNavigatorWindow window;
    JComponent component;
    INavigatorController controller;

    public NavigatorView(ClientNavigatorWindow window, JComponent component, INavigatorController controller) {
        super(component);
        this.window = window;
        this.component = component;
        this.controller = controller;
    }

    public abstract void refresh(Set<ClientNavigatorElement> newElements);

    public abstract ClientNavigatorElement getSelectedElement();

}
