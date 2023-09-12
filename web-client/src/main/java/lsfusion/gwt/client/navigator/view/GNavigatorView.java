package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;

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

        // we want to propagate this classes, since window hover classes are also propagated
        parentRecentlySelected = new RecentlyEventClassHandler(component, true, "parent-was-selected-recently", 2000);
        recentlySelected = new RecentlyEventClassHandler(component, true, "was-selected-recently", 1000);
    }

    private RecentlyEventClassHandler parentRecentlySelected;
    private RecentlyEventClassHandler recentlySelected;

    public Widget getView() {
        return component;
    }

    public Widget getComponent() {
        return window.drawScrollBars ? ((ScrollPanel) component).getWidget() : component;
    }

    public abstract void refresh(LinkedHashSet<GNavigatorElement> newElements);

    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    public abstract int getHeight();

    public abstract int getWidth();

    public void onParentSelected() {
        parentRecentlySelected.onEvent();
    }
    public void onSelected() {
        recentlySelected.onEvent();
    }
    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
    }
    
    public void openFirstFolder() {
    }

    protected void selectElement(GNavigatorElement element, NativeEvent event) {
        if(element instanceof GNavigatorFolder) {
            navigatorController.resetSelectedElements(element);
            selected = element;

            navigatorController.update();

            navigatorController.onSelectedElement(element);
        } else
            navigatorController.openElement((GNavigatorAction) element, event);
    }
}
