package lsfusion.client.navigator.view;

import lsfusion.client.form.design.view.widget.ScrollPaneWidget;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.window.ClientNavigatorWindow;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public abstract class NavigatorView {
    public ClientNavigatorWindow window;
    protected JComponent component;
    protected INavigatorController controller;

    public NavigatorView(ClientNavigatorWindow window, JComponent iComponent, INavigatorController controller) {
        this.window = window;
        
        if (window.drawScrollBars) {
            component = new ScrollPaneWidget(iComponent);
            ((ScrollPaneWidget) component).getVerticalScrollBar().setUnitIncrement(14);
            ((ScrollPaneWidget) component).getHorizontalScrollBar().setUnitIncrement(14);
        } else {
            component = iComponent;   
        }
        component.setBorder(BorderFactory.createEmptyBorder());
        
        this.controller = controller;
    }

    public JComponent getView() {
        return component;
    }

    public Component getComponent() {
        return window.drawScrollBars ? ((ScrollPaneWidget) component).getViewport().getView() : component;
    }

    public abstract void refresh(Set<ClientNavigatorElement> newElements);

    public abstract ClientNavigatorElement getSelectedElement();

    public void resetSelectedElement(ClientNavigatorElement newSelectedElement) {
    }

    public void openFirstFolder() {
    }
}
