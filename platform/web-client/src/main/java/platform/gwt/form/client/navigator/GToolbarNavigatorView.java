package platform.gwt.form.client.navigator;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.base.client.ui.ResizableHorizontalPanel;
import platform.gwt.base.client.ui.ResizableVerticalPanel;
import platform.gwt.form.shared.view.GNavigatorElement;
import platform.gwt.form.shared.view.panel.ImageButton;
import platform.gwt.form.shared.view.window.GToolbarNavigatorWindow;

import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView {
    private static final int PADDING_STEP = 15;
    private CellPanel panel;
    private boolean vertical;
    private HasVerticalAlignment.VerticalAlignmentConstant alignmentY;
    private HasAlignment.HorizontalAlignmentConstant alignmentX;
    private boolean verticalTextAlign;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        alignmentX = window.getAlignmentX();
        alignmentY = window.getAlignmentY();
        verticalTextAlign = window.hasVerticalTextPosition();

        vertical = window.type == 1;
        panel = vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        SimplePanel toolbarContainer = new SimplePanel(panel);
        if (vertical) {
            toolbarContainer.setStyleName("verticaToolbar");
            panel.setWidth("100%");
        } else {
            toolbarContainer.setStyleName("horizontalToolbar");
            panel.setHeight("100%");
        }
        setComponent(toolbarContainer);
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

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step) {
        ImageButton button = new ImageButton(element.caption, verticalTextAlign);
        button.setImage(element.icon);
        button.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                selected = element;
                navigatorController.update();
                navigatorController.openElement(element);
            }
        });

        button.setHeight("auto");
        button.addStyleName("toolbarNavigatorButton");
        if (element.equals(selected)) {
            button.addStyleName("toolbarSelectedNavigatorButton");
        }
        if (vertical) {
            button.getElement().getStyle().setPaddingLeft(7 + PADDING_STEP * step, Style.Unit.PX);
        }

        panel.add(button);
        panel.setCellVerticalAlignment(button, alignmentY);
        panel.setCellHorizontalAlignment(button, alignmentX);

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
