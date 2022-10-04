package lsfusion.gwt.client.navigator.view;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GPanelNavigatorWindow;

import java.util.Set;

public class GPanelNavigatorView extends GNavigatorView {
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

        CaptionPanel titledPanel = new CaptionPanel(element.caption, insidePanel);
        titledPanel.setSize("100%", "100%");
        container.add(titledPanel);
    }

    private FormButton createButton(final GNavigatorElement element) {
        FormButton button = new NavigatorImageButton(element, false);
        button.addStyleName("panelNavigatorView");

        button.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                selected = element;
                navigatorController.update();
                navigatorController.openElement(element, event.getNativeEvent());
            }
        });
        return button;
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    @Override
    public int getHeight() {
        return panel.getOffsetHeight();
    }

    @Override
    public int getWidth() {
        return panel.getOffsetWidth();
    }
}
