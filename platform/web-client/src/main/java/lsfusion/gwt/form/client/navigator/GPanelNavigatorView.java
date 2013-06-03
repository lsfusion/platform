package lsfusion.gwt.form.client.navigator;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;
import lsfusion.gwt.form.shared.view.GNavigatorElement;
import lsfusion.gwt.form.shared.view.panel.ImageButton;
import lsfusion.gwt.form.shared.view.window.GPanelNavigatorWindow;

import java.util.Set;

public class GPanelNavigatorView extends GNavigatorView {
    private CellPanel panel;

    public GPanelNavigatorView(GPanelNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        panel = window.orientation == 1 ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
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

        if (element instanceof GNavigatorForm) {
            insidePanel.add(createButton(element));
        } else {
            for (GNavigatorElement child : element.children) {
                if (!child.children.isEmpty()) {
                    addElement(child, insidePanel);
                } else {
                    insidePanel.add(createButton(child));
                }
            }
        }

        GCaptionPanel titledPanel = new GCaptionPanel(element.caption, insidePanel);
        container.add(titledPanel);
    }

    private Button createButton(final GNavigatorElement element) {
        ImageButton button = new ImageButton(element.caption, element.icon);
        button.addStyleName("panelNavigatorView");

        button.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                selected = element;
                navigatorController.update();
                navigatorController.openElement(element);
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
