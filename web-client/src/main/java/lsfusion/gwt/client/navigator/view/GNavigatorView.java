package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.Set;

public abstract class GNavigatorView<T extends GNavigatorWindow> {
    protected T window;
    protected Widget component;
    protected GINavigatorController navigatorController;
    protected GNavigatorElement selected;

    public GNavigatorView(T window, GINavigatorController navigatorController) {
        this.window = window;
        this.navigatorController = navigatorController;
    }

    public GNavigatorView(T window, Widget component, GINavigatorController navigatorController) {
        this(window, navigatorController);
        setComponent(component);
    }

    public void setComponent(Widget component) {
        this.component = component;
    }

    public Widget getView() {
        return component;
    }

    public Widget getComponent() {
        return window.drawScrollBars ? ((ScrollPanel) component).getWidget() : component;
    }

    public abstract void refresh(Set<GNavigatorElement> newElements);

    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    public abstract int getHeight();

    public abstract int getWidth();

    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
    }
    
    public void openFirstFolder() {
    }

    protected void selectElement(GNavigatorElement element, NativeEvent event) {
        if(element instanceof GNavigatorFolder) {
            navigatorController.resetSelectedElements(element);
            selected = element;

            navigatorController.update();
        } else
            navigatorController.openElement((GNavigatorAction) element, event);
    }
}
