package lsfusion.gwt.client.navigator.view;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GPanelNavigatorWindow;

import java.util.Set;

import static lsfusion.gwt.client.navigator.view.GToolbarNavigatorView.getChildrenMaxSize;
import static lsfusion.gwt.client.navigator.view.GToolbarNavigatorView.getScrollbarSizeIfNeeded;

public class GPanelNavigatorView extends GNavigatorView<GPanelNavigatorWindow> {
    private CellPanel panel;

    public GPanelNavigatorView(GPanelNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        panel = window.isVertical() ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        setComponent(panel);
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        panel.clear();
        for (final GNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, panel);
            }
        }
    }

    private void addElement(GNavigatorElement element, Panel container) {
        ResizableHorizontalPanel insidePanel = new ResizableHorizontalPanel();

        for (GNavigatorElement child : element.children) {
            if (!child.children.isEmpty()) {
                addElement(child, insidePanel);
            } else {
                insidePanel.add(createButton(child));
            }
        }

        CaptionPanel titledPanel = new CaptionPanel(element.caption, element.image, insidePanel);
        titledPanel.setSize("100%", "100%");
        container.add(titledPanel);
    }

    private FormButton createButton(final GNavigatorElement element) {
        FormButton button = new NavigatorImageButton(element, false);
        button.addStyleName("panelNavigatorView");

        button.addMouseDownHandler(event -> selectElement(element, event.getNativeEvent()));
        return button;
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
        } else {
            return super.getAutoSize(vertical);
        }
    }
}
