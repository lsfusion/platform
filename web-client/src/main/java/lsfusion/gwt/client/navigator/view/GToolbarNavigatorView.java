package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

import java.util.LinkedHashSet;
import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView<GToolbarNavigatorWindow> {

    private final ResizableComplexPanel panel;
    private final boolean verticalTextAlign;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);

        verticalTextAlign = window.hasVerticalTextPosition();

        boolean vertical = window.isVertical();

        ToolbarPanel main = new ToolbarPanel(vertical, window);

        setComponent(main);

        this.panel = main.panel;
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> newElements) {
        for(int i = 0, size = panel.getWidgetCount(); i < size; i++) {
            ((NavigatorImageButton) panel.getWidget(i)).destroyTooltip();
        }

        Result<Integer> index = new Result<>(0);
        for (GNavigatorElement newElement : newElements) {
            if (!newElements.contains(newElement.parent)) { // only root components, since children are added recursively
                if (firstFolder == null && newElement.isFolder() && window.isRoot()) {
                    firstFolder = newElement;
                }
                addElement(newElement, newElements, 0, index);
            }
        }

        for(int i = index.result, size = panel.getWidgetCount(); i < size; i++)
            panel.remove(index.result);
    }

    //open first folder at start
    GNavigatorElement firstFolder = null;

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step, Result<Integer> index) {
        boolean active = window.allButtonsActive() || (element.isFolder() && element.equals(selected));

        if(index.result < panel.getWidgetCount()) {
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

    @Override
    public int getHeight() {
        return panel.getElement().getScrollHeight();
    }

    @Override
    public int getWidth() {
        return panel.getElement().getScrollWidth();
    }

    @Override
    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
        GNavigatorElement selectedElement = getSelectedElement();
        if(selectedElement != null && selectedElement.findChild(newSelectedElement) == null) {
            selected = null;
        }
    }

    @Override
    public void openFirstFolder() {
        super.openFirstFolder();

        if (firstFolder != null) {
            selectElement(firstFolder, null);
            firstFolder = null;
        }
    }
}
