package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GPanelNavigatorWindow;

import java.util.LinkedHashSet;

public class GPanelNavigatorView extends GNavigatorView<GPanelNavigatorWindow> {
    private CellPanel panel;

    public GPanelNavigatorView(GPanelNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        panel = window.isVertical() ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        setComponent(panel);
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> newElements) {
        panel.clear();
        for (final GNavigatorElement element : newElements) {
            if (!newElements.contains(element.parent)) {
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
        FormButton button = new NavigatorImageButton(element, false, 0, false, this::selectElement);
        button.addStyleName("panelNavigatorView");
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
}
