package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;
import java.util.Set;

public class GNavigatorView implements GINavigatorView {

    private GNavigatorWindow window;
    private Widget component;
    private GINavigatorController navigatorController;

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

    @Override
    public Widget getView() {
        return component;
    }

    private void setComponent(Widget component) {
        this.component = component;

        GFormLayout.setDebugInfo(component, window.canonicalName);

        // we want to propagate this classes, since window hover classes are also propagated
        parentRecentlySelected = new RecentlyEventClassHandler(component, true, "parent-was-selected-recently", 2000);
        recentlySelected = new RecentlyEventClassHandler(component, true, "was-selected-recently", 1000);
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> newElements, GNavigatorElement selected) {
        Result<Integer> index = new Result<>(0);
        for (GNavigatorElement newElement : newElements) {
            if (!newElements.contains(newElement.parent)) { // only root components, since children are added recursively
                addElement(newElement, newElements, selected, 0, index);
            }
        }

        for (int i = index.result, size = panel.getWidgetCount(); i < size; i++)
            panel.remove(index.result);
    }

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, GNavigatorElement selected, int step, Result<Integer> index) {
        if (!element.hide) {
            boolean active = window.allButtonsActive() || (element.isFolder() && element.equals(selected));

            if (index.result < panel.getWidgetCount()) {
                NavigatorImageButton button = (NavigatorImageButton) panel.getWidget(index.result);
                button.change(element, step, active);
            } else {
                NavigatorImageButton button = new NavigatorImageButton(element, verticalTextAlign, step, active, navigatorController::activate);
                panel.add(button);
            }
            index.set(index.result + 1);

            if (element.window == null || element.window.equals(window)) {
                for (GNavigatorElement childEl : element.children) {
                    if (newElements.contains(childEl)) {
                        addElement(childEl, newElements, selected, step + 1, index);
                    }
                }
            }
        }
    }

    private RecentlyEventClassHandler parentRecentlySelected;
    private RecentlyEventClassHandler recentlySelected;

    @Override
    public void onParentSelected() {
        parentRecentlySelected.onEvent();
    }

    @Override
    public void onSelected() {
        recentlySelected.onEvent();
    }
}
