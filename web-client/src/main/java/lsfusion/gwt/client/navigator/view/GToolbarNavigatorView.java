package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView<GToolbarNavigatorWindow> {
    private final Panel main;

    private final ResizableComplexPanel panel;
    private final boolean verticalTextAlign;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);

        verticalTextAlign = window.hasVerticalTextPosition();

        boolean vertical = window.isVertical();
        main = new ResizableComplexPanel();
        main.addStyleName("navbar p-0");

        panel = new ResizableComplexPanel();
        panel.addStyleName("navbar-nav");
        panel.addStyleName("navbar-nav-" + (vertical ? "vert" : "horz"));

        if (vertical) {
            panel.addStyleName(window.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                              (window.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ?  "align-items-end" :
                                                                                               "align-items-start"));

            main.addStyleName(window.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                              (window.alignmentY == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ?  "justify-content-end" :
                                                                                                        "justify-content-start"));
        } else {
            panel.addStyleName(window.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                              (window.alignmentY == GToolbarNavigatorWindow.BOTTOM_ALIGNMENT ? "align-items-end" :
                                                                                               "align-items-start"));

            main.addStyleName(window.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                              (window.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ?  "justify-content-end" :
                                                                                                      "justify-content-start"));
        }
        main.add(panel);

        setComponent(main);
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
        FormButton button = new NavigatorImageButton(element, verticalTextAlign);
        button.addStyleName("nav-item");
        button.addStyleName("nav-link");
        button.addStyleName("navbar-text");

        button.addStyleName(verticalTextAlign ? "nav-link-vert" : "nav-link-horz");
        button.addStyleName((verticalTextAlign ? "nav-link-vert" : "nav-link-horz") + "-" + step);

        // debug info
        button.getElement().setAttribute("lsfusion-container", element.canonicalName);

        button.addClickHandler(event -> click(element, event.getNativeEvent()));

        TooltipManager.TooltipHelper tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return element.getTooltipText();
            }

            @Override
            public String getPath() {
                return element.path;
            }

            @Override
            public String getCreationPath() {
                return element.creationPath;
            }

            @Override
            public boolean stillShowTooltip() {
                return button.isAttached() && button.isVisible();
            }
        };
        TooltipManager.registerWidget(button, tooltipHelper);

        if (window.allButtonsActive() || (element.isFolder() && element.equals(selected))) {
            button.addStyleName("active");
        }

        if (element.isFolder() && window.isRoot() && firstFolder) {
            firstFolder = false;
            click(element, null);
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

    public void click(GNavigatorElement element, NativeEvent event) {
        navigatorController.resetSelectedElements(element);
        selected = element;
        navigatorController.update();
        navigatorController.openElement(element, event);
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return selected;
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
    public int getAutoSize(boolean vertical) {
        if (vertical != window.isVertical()) {
            // as panel is stretched to the window size, the size returned for panel is the size of the window.
            int result = getChildrenMaxSize(vertical, panel);

            result += getScrollbarSizeIfNeeded(window.isVertical(), panel);
            return result;
        }
        return super.getAutoSize(vertical);
    }
    
    // if size provided by window is larger than total size (on main axis) of toolbar, a scrollbar is expected to appear
    // add scrollbar width to the cross-axis size
    // maybe should be adjusted after redraw as current window sizes are considered - after update no scrollbar may appear
    public static int getScrollbarSizeIfNeeded(boolean vertical, Widget widget) {
        int childrenTotalSize = vertical ? widget.getElement().getScrollHeight() : widget.getElement().getScrollWidth();
        int parentSize = vertical ? widget.getOffsetHeight() : widget.getOffsetWidth();
        if (childrenTotalSize > parentSize) {
            return AbstractNativeScrollbar.getNativeScrollbarHeight();
        }
        return 0;            
    }

    public static int getChildrenMaxSize(boolean vertical, ComplexPanel panel) {
        int size = 0;
        for (int i = 0; i < panel.getWidgetCount(); i++) {
            Widget child = panel.getWidget(i);
            size = Math.max(size, vertical ? child.getOffsetHeight() : child.getOffsetWidth());
        }
        return size;
    }

    @Override
    public void resetSelectedElement(GNavigatorElement newSelectedElement) {
        GNavigatorElement selectedElement = getSelectedElement();
        if(selectedElement != null && selectedElement.findChild(newSelectedElement) == null) {
            selected = null;
        }
    }
}
