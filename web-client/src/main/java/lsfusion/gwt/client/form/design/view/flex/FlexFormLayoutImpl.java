package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.GFormLayoutImpl;
import lsfusion.gwt.client.form.design.view.ScrollContainerView;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class FlexFormLayoutImpl extends GFormLayoutImpl {

    @Override
    public GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.isLinear()) {
            return new FlexLinearContainerView(container);
        } else if (container.isSplit()) {
            return new FlexSplitContainerView(container);
        } else if (container.isTabbed()) {
            return new FlexTabbedContainerView(form, container);
        } else if (container.isColumns()) {
            return new FlexColumnsContainerView(container);
        } else if(container.isScroll()) {
            return new ScrollContainerView(container);
        } else {
            throw new IllegalStateException("Incorrect container type");
        }
    }

    @Override
    public void setupMainContainer(Widget mainContainerWidget) {
        Element mainContainerElement = mainContainerWidget.getElement();
        mainContainerElement.getStyle().setOverflow(Style.Overflow.AUTO);
        setupFillParent(mainContainerElement);
    }

    public static class GridPanel extends FlexPanel {
        private final ResizableSimplePanel panel;

        private GGridPropertyTable getGridTable() {
            return (GGridPropertyTable) panel.getWidget();
        }

        public GridPanel(ResizableSimplePanel panel) {
            super(true);

            this.panel = panel;
            addFill(panel);
        }

        public void autoSize() {
            GGridPropertyTable gridTable = getGridTable();
            int autoSize;
            if (gridTable instanceof GTreeTable) {
                autoSize = gridTable.getMaxPreferredSize().height;    
            } else {
                autoSize = gridTable.getAutoSize();
                if (autoSize <= 0) // еще не было layout'а, ставим эвристичный размер
                    autoSize = gridTable.getMaxPreferredSize().height;
                else {
                    autoSize += panel.getOffsetHeight() - gridTable.getTableDataScroller().getClientHeight(); // margin'ы и border'ы учитываем
                }
            }
            setChildFlexBasis(panel, autoSize);
        }
    }

    @Override
    public Panel createGridView(GGrid grid, ResizableSimplePanel panel) {
        return new GridPanel(panel);
    }

    @Override
    public Panel createTreeView(GTreeGroup treeGroup, ResizableSimplePanel panel) {
        return new GridPanel(panel);
    }
}
