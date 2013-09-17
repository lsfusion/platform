package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.ResizableLayoutPanel;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.List;
import java.util.Map;

public class GGridController {
    private GGrid grid;
    private GridView gridView;
    private GGridTable table;
    private GGroupObjectController groupController;
    private boolean forceHidden = false;

    public GGridController(GGrid igrid, GFormController iformController, GGroupObjectController igroupObject) {
        grid = igrid;
        groupController = igroupObject;

        gridView = new GridView();

        table = new GGridTable(iformController, igroupObject, this);

        ResizableLayoutPanel panel = new ResizableLayoutPanel();
        panel.setStyleName("gridResizePanel");

        panel.setWidget(table);
        gridView.addFill(panel);
    }

    public GGridTable getTable() {
        return table;
    }

    public GPropertyDraw getCurrentProperty() {
        return table.getCurrentProperty();
    }

    public Object getSelectedValue(GPropertyDraw property) {
        return table.getSelectedValue(property);
    }

    public void setForceHidden(boolean hidden) {
        forceHidden = hidden;
    }
    
    public GFont getFont() {
        return grid.font;
    }

    public boolean isVisible() {
        return !forceHidden && groupController.isInGridClassView();
    }

    public void update() {
        table.update();
        boolean oldGridVisibilityState = gridView.isVisible();
        if (oldGridVisibilityState != isVisible()) {
            gridView.setVisible(isVisible());
            groupController.formController.setNeedToResize(true);
        }
    }

    void restoreScrollPosition() {
        table.restoreScrollPosition();
    }

    public void beforeHiding() {
        table.beforeHiding();
    }

    void afterShowing() {
        table.afterShowing();
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(grid, gridView);
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellBackgroundValues(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellForegroundValues(propertyDraw, values);
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updatePropertyCaptions(propertyDraw, values);
    }

    public void updateShowIfValues(GPropertyDraw property, Map<GGroupObjectValue, Object> values) {
        table.updateShowIfValues(property, values);
    }

    public void updateReadOnlyValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateReadOnlyValues(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowBackgroundValues(values);
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowForegroundValues(values);
    }

    public void modifyGridObject(GGroupObjectValue key, boolean add) {
        table.modifyGroupObject(key, add);
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        table.updateColumnKeys(property, columnKeys);
    }

    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        table.changeOrder(property, modiType);
    }

    public void selectProperty(GPropertyDraw propertyDraw) {
        table.selectProperty(propertyDraw);
    }

    public void clearGridOrders(GGroupObject groupObject) {
        table.clearGridOrders(groupObject);
    }

    private class GridView extends FlexPanel implements DefaultFocusReceiver {
        public GridView() {
            super(true);
            setMargins(grid.marginTop, grid.marginBottom, grid.marginLeft, grid.marginRight);
        }

        @Override
        public boolean focus() {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    table.setFocus(true);
                }
            });
            return true;
        }
    }
}
