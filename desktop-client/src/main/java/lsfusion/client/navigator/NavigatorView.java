package lsfusion.client.navigator;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public abstract class NavigatorView {
    public ClientNavigatorWindow window;
    JComponent component;
    INavigatorController controller;

    public NavigatorView(ClientNavigatorWindow window, JComponent iComponent, INavigatorController controller) {
        this.window = window;
        
        if (window.drawScrollBars) {
            component = new JScrollPane(iComponent);
            ((JScrollPane) component).getVerticalScrollBar().setUnitIncrement(14);
            ((JScrollPane) component).getHorizontalScrollBar().setUnitIncrement(14);
        } else {
            component = iComponent;   
        }
        
        this.controller = controller;
    }

    public JComponent getView() {
        return component;
    }

    public Component getComponent() {
        return window.drawScrollBars ? ((JScrollPane) component).getViewport().getView() : component;
    }

    public abstract void refresh(Set<ClientNavigatorElement> newElements);

    public abstract ClientNavigatorElement getSelectedElement();

}
