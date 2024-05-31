package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;
import java.util.Set;

public class GNavigatorView {

    private GNavigatorWindow window;
    private Widget component;
    private GINavigatorController navigatorController;
    private GNavigatorElement selected;

    private ResizableComplexPanel panel;
    private boolean verticalTextAlign;

    public GNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        this.window = window;
        this.navigatorController = navigatorController;

        verticalTextAlign = window.hasVerticalTextPosition();

        boolean vertical = window.isVertical();

        ToolbarPanel main = new ToolbarPanel(vertical, window);

        setComponent(main);

        this.panel = main.panel;
    }

    public Widget getView() {
        return component;
    }

    public Widget getComponent() {
        return window.drawScrollBars ? ((ScrollPanel) component).getWidget() : component;
    }

    public void setComponent(Widget component) {
        this.component = component;

        GFormLayout.setDebugInfo(component, window.canonicalName);

        // we want to propagate this classes, since window hover classes are also propagated
        parentRecentlySelected = new RecentlyEventClassHandler(component, true, "parent-was-selected-recently", 2000);
        recentlySelected = new RecentlyEventClassHandler(component, true, "was-selected-recently", 1000);
    }

    public void refresh(LinkedHashSet<GNavigatorElement> newElements) {
        Result<Integer> index = new Result<>(0);
        for (GNavigatorElement newElement : newElements) {
            if (!newElements.contains(newElement.parent)) { // only root components, since children are added recursively
                if (firstFolder == null && newElement.isFolder() && window.isRoot()) {
                    firstFolder = newElement;
                }
                addElement(newElement, newElements, 0, index);
            }
        }

        for (int i = index.result, size = panel.getWidgetCount(); i < size; i++)
            panel.remove(index.result);
    }

    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    //open first folder at start
    GNavigatorElement firstFolder = null;

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step, Result<Integer> index) {
        boolean active = window.allButtonsActive() || (element.isFolder() && element.equals(selected));

        if (index.result < panel.getWidgetCount()) {
            NavigatorImageButton button = (NavigatorImageButton) panel.getWidget(index.result);
            button.change(element, step, active);
        } else {
            NavigatorImageButton button = new NavigatorImageButton(element, verticalTextAlign, step, active, this::selectElement);
            panel.add(button);
        }
        index.set(index.result + 1);

        if (element.window == null || element.window.equals(window)) {
            for (GNavigatorElement childEl : element.children) {
                if (newElements.contains(childEl)) {
                    addElement(childEl, newElements, step + 1, index);
                }
            }
        }
    }

    private RecentlyEventClassHandler parentRecentlySelected;
    private RecentlyEventClassHandler recentlySelected;

    public void onParentSelected() {
        parentRecentlySelected.onEvent();
    }

    public void onSelected() {
        recentlySelected.onEvent();
    }

    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
        GNavigatorElement selectedElement = getSelectedElement();
        if (selectedElement != null && selectedElement.findChild(newSelectedElement) == null) {
            selected = null;
        }
    }

    public void openFirstFolder() {
        if (firstFolder != null) {
            selectElement(firstFolder, null);
            firstFolder = null;
        }
    }

    private void selectElement(GNavigatorElement element, NativeEvent event) {
        if (element instanceof GNavigatorFolder) {
            navigatorController.resetSelectedElements(element);
            selected = element;

            navigatorController.update();

            navigatorController.onSelectedElement(element);
        } else
            navigatorController.openElement((GNavigatorAction) element, event);
    }
}
