package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView<GToolbarNavigatorWindow> {
    private final ResizableComplexPanel main;

    private final ResizableComplexPanel panel;
    private final boolean verticalTextAlign;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);

        verticalTextAlign = window.hasVerticalTextPosition();

        boolean vertical = window.isVertical();

        Pair<ResizableComplexPanel, ResizableComplexPanel> toolbarPanel = createToolbarPanel(vertical);
        this.main = toolbarPanel.first;
        this.panel = toolbarPanel.second;

        setAlignment(vertical, this.main, this.panel, window);

        setComponent(this.main);
    }

    public static Pair<ResizableComplexPanel, ResizableComplexPanel> createToolbarPanel(boolean vertical) {
        ResizableComplexPanel main = new ResizableComplexPanel();
        main.addStyleName("navbar navbar-expand p-0"); // navbar-expand to set horizontal paddings (vertical are set in navbar-text)

        main.addStyleName("navbar-" + (vertical ? "vert" : "horz"));
        
        ResizableComplexPanel panel = new ResizableComplexPanel();
        panel.addStyleName("navbar-nav");
        panel.addStyleName(vertical ? "navbar-nav-vert" : "navbar-nav-horz");

        main.add(panel);
        return new Pair<>(main, panel);
    }

    public static void setAlignment(boolean vertical, ResizableComplexPanel main, ResizableComplexPanel panel, GToolbarNavigatorWindow toolbarWindow) {
        if (vertical) {
            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        } else {
            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.BOTTOM_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        }
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        panel.clear();

        for (GNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, newElements, 0);
            }
        }
    }

    //open first folder at start
    boolean firstFolder = true;

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step) {
        FormButton button = new NavigatorImageButton(element, verticalTextAlign, step);

        button.addClickHandler(event -> selectElement(element, event.getNativeEvent()));

        if (window.allButtonsActive() || (element.isFolder() && element.equals(selected))) {
            button.addStyleName("active");
        }

        if (element.isFolder() && window.isRoot() && firstFolder) {
            firstFolder = false;
            selectElement(element, null);
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
}
