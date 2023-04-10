package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

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
    public void refresh(Set<GNavigatorElement> newElements) {
        panel.clear();

        for (GNavigatorElement element : newElements) {
            if (!newElements.contains(element.parent)) {
                addElement(element, newElements, 0);
            }
        }
    }

    //open first folder at start
    GNavigatorElement firstFolder = null;

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step) {
        FormButton button = new NavigatorImageButton(element, verticalTextAlign, step);

        button.addClickHandler(event -> selectElement(element, event.getNativeEvent()));

        if (window.allButtonsActive() || (element.isFolder() && element.equals(selected))) {
            button.addStyleName("active");
        }

        if (firstFolder == null && element.isFolder() && window.isRoot()) {
            firstFolder = element;
        }

        panel.add(button);

        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (GNavigatorElement childEl: element.children) {
            if (newElements.contains(childEl)) {
                addElement(childEl, newElements, step + 1);
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
