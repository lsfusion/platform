package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.ResizableComplexPanel;
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

import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;

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
        } else {
            throw new IllegalStateException("Incorrect container type");
        }
    }

    @Override
    public void setupMainContainer(Widget mainContainerWidget) {
        setupFillParent(mainContainerWidget.getElement());
    }

    @Override
    public Panel createGridView(GGrid grid, Panel panel) {
        GridView gridView = new GridView(grid);
        gridView.addFill(panel);
        return gridView;
    }

    @Override
    public GPanelController.RenderersPanel createRenderersPanel() {
        return new RenderersPanel();
    }

    @Override
    public void setupActionPanelRenderer(GPanelController.GPropertyController controller, ActionPanelRenderer actionRenderer) {
        Widget renderersPanel = controller.renderersPanel.asWidget();
        Widget parent = renderersPanel.getParent();
        if (parent instanceof FlexPanel) {
            FlexPanel flexParent = (FlexPanel) parent;
            boolean isVertical = flexParent.isVertical();
            boolean isStretched = flexParent.getChildAlignment(renderersPanel) == GFlexAlignment.STRETCH;
            boolean isFlexed = flexParent.getChildFlex(renderersPanel) > 0;

            ImageButton button = actionRenderer.getButton();
            if ((isVertical && isFlexed) || (!isVertical && isStretched)) {
                button.getElement().getStyle().clearHeight();
            }
            if ((!isVertical && isFlexed) || (isVertical && isStretched)) {
                button.getElement().getStyle().clearWidth();
            }
        }
    }

    @Override
    public void setupDataPanelRenderer(GPanelController.GPropertyController controller, DataPanelRenderer dataRenderer) {
        Widget renderersPanel = controller.renderersPanel.asWidget();
        Widget parent = renderersPanel.getParent();
        if (parent instanceof FlexPanel) {
            FlexPanel flexParent = (FlexPanel) parent;
            boolean isVertical = flexParent.isVertical();
            boolean isStretched = flexParent.getChildAlignment(renderersPanel) == GFlexAlignment.STRETCH;
            boolean isFlexed = flexParent.getChildFlex(renderersPanel) > 0;

            FlexPanel panel = dataRenderer.panel;
            ResizableComplexPanel gridPanel = dataRenderer.gridPanel;
            GSinglePropertyTable valueTable = dataRenderer.valueTable;
            if ((isVertical && isFlexed) || (!isVertical && isStretched)) {
                panel.setChildAlignment(gridPanel, GFlexAlignment.STRETCH);

                gridPanel.getElement().getStyle().clearHeight();
                gridPanel.getElement().getStyle().clearProperty("minHeight");
                gridPanel.getElement().getStyle().clearProperty("maxHeight");
                gridPanel.getElement().getStyle().setPosition(Style.Position.RELATIVE);

                valueTable.setupFillParent();
            }

            if ((!isVertical && isFlexed) || (isVertical && isStretched)) {
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

    private static class GridView extends FlexPanel {
        public GridView(GGrid grid) {
            super(true);
            setMargins(grid.marginTop, grid.marginBottom, grid.marginLeft, grid.marginRight);
        }
    }
}
