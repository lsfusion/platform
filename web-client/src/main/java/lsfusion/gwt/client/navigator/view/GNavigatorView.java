package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.Set;

public abstract class GNavigatorView {
    protected GNavigatorWindow window;
    protected Widget component;
    protected GINavigatorController navigatorController;
    protected GNavigatorElement selected;

    public GNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        this.window = window;
        this.navigatorController = navigatorController;
    }

    public GNavigatorView(GNavigatorWindow window, Widget component, GINavigatorController navigatorController) {
        this(window, navigatorController);
        setComponent(component);
    }

    public void setComponent(Widget component) {
        this.component = window.drawScrollBars ? new ScrollPanel(component) : component;
    }

    public Widget getView() {
        return component;
    }

    public Widget getComponent() {
        return window.drawScrollBars ? ((ScrollPanel) component).getWidget() : component;
    }

    public abstract void refresh(Set<GNavigatorElement> newElements);

    public abstract GNavigatorElement getSelectedElement();

    public abstract int getHeight();

    public abstract int getWidth();

    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
    }
}
