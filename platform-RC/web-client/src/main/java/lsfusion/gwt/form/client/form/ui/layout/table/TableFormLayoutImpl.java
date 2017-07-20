package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.client.form.ui.layout.ScrollContainerView;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GGrid;

public class TableFormLayoutImpl extends GFormLayoutImpl {
    @Override
    public GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.isLinear()) {
            return new TableLinearContainerView(container);
        } else if (container.isSplit()) {
            return new TableSplitContainerView(container);
        } else if (container.isTabbed()) {
            return new TableTabbedContainerView(form, container);
        } else if (container.isColumns()) {
            return new TableColumnsContainerView(container);
        } else if (container.isScroll()) {
            return new ScrollContainerView(container);
        } else {
            throw new IllegalStateException("Incorrect container type");
        }
    }

    @Override
    public void setupMainContainer(Widget mainContainerWidget) {
        Style style = mainContainerWidget.getElement().getStyle();
        style.setWidth(100, Style.Unit.PCT);
        style.setHeight(100, Style.Unit.PCT);
    }

    @Override
    public Panel createGridView(GGrid grid, Panel panel) {
        ResizableVerticalPanel gridView = new ResizableVerticalPanel();
        gridView.add(panel);

        panel.setSize("100%", "100%");
        gridView.setCellWidth(panel, "100%");
        gridView.setCellHeight(panel, "100%");

        return gridView;
    }
}
