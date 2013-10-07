package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.ResizableComplexPanel;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GPanelController;
import lsfusion.gwt.form.client.form.ui.GSinglePropertyTable;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GGrid;
import lsfusion.gwt.form.shared.view.panel.ActionPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.DataPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.ImageButton;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

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
        GridView gridView = new GridView(grid);
        gridView.add(panel);

        panel.setSize("100%", "100%");
        gridView.setCellWidth(panel, "100%");
        gridView.setCellHeight(panel, "100%");

        return gridView;
    }

    @Override
    public GPanelController.RenderersPanel createRenderersPanel() {
        return new RenderersPanel();
    }

    @Override
    public void setupActionPanelRenderer(GPanelController.GPropertyController controller, ActionPanelRenderer actionRenderer) {
        Element parentElement = controller.renderersPanel.asWidget().getElement().getParentElement();
        if (parentElement != null && "td".equalsIgnoreCase(parentElement.getTagName())) {
            String width = parentElement.getAttribute("width");
            String height = parentElement.getAttribute("height");
            boolean horzStretched = width != null && width.trim().endsWith("%");
            boolean vertStretched = height != null && height.trim().endsWith("%");

            ImageButton button = actionRenderer.getButton();
            if (vertStretched) {
                button.getElement().getStyle().clearHeight();
            }
            if (horzStretched) {
                button.getElement().getStyle().clearWidth();
            }
        }
    }

    @Override
    public void setupDataPanelRenderer(GPanelController.GPropertyController controller, DataPanelRenderer dataRenderer) {
        Element parentElement = controller.renderersPanel.asWidget().getElement().getParentElement();
        if (parentElement != null && "td".equalsIgnoreCase(parentElement.getTagName())) {
            String width = parentElement.getAttribute("width");
            String height = parentElement.getAttribute("height");
            boolean horzStretched = width != null && width.trim().endsWith("%");
            boolean vertStretched = height != null && height.trim().endsWith("%");

            FlexPanel panel = dataRenderer.panel;
            ResizableComplexPanel gridPanel = dataRenderer.gridPanel;
            GSinglePropertyTable valueTable = dataRenderer.valueTable;
            if (vertStretched) {
                panel.setChildAlignment(gridPanel, GFlexAlignment.STRETCH);

                gridPanel.getElement().getStyle().clearHeight();
                gridPanel.getElement().getStyle().clearProperty("minHeight");
                gridPanel.getElement().getStyle().clearProperty("maxHeight");
                gridPanel.getElement().getStyle().setPosition(Style.Position.RELATIVE);

                valueTable.setupFillParent();
            }

            if (horzStretched) {
                gridPanel.getElement().getStyle().clearWidth();
                gridPanel.getElement().getStyle().clearProperty("minWidth");
                gridPanel.getElement().getStyle().clearProperty("maxWidth");
            }
        }
    }

    private static class RenderersPanel extends FlexPanel implements GPanelController.RenderersPanel {
        @Override
        public void add(PanelRenderer renderer) {
            add(renderer.getComponent(), GFlexAlignment.STRETCH, 1, "auto");
        }

        @Override
        public void remove(PanelRenderer renderer) {
            remove(renderer.getComponent());
        }
    }

    private class GridView extends ResizableVerticalPanel {
        public GridView(GGrid grid) {
            //todo: margins
//            setMargins(grid.marginTop, grid.marginBottom, grid.marginLeft, grid.marginRight);
        }
    }
}
