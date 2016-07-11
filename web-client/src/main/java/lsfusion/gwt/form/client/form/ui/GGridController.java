package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.base.client.ui.ResizableSimplePanel;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.client.form.ui.toolbar.preferences.GGridUserPreferences;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.List;
import java.util.Map;

import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;

public class GGridController {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();
    
    private GGrid grid;
    private Panel gridView;
    private GGridTable table;
    private GGroupObjectController groupController;
    private boolean forceHidden = false;

    public GGridController(GGrid igrid, GFormController iformController, GGroupObjectController igroupObject, GGridUserPreferences[] userPreferences) {
        grid = igrid;
        groupController = igroupObject;

        table = new GGridTable(iformController, igroupObject, this, userPreferences);

//        ResizableLayoutPanel panel = new ResizableLayoutPanel();
//        panel.setStyleName("gridResizePanel");
//        panel.setWidget(table);

        ResizableSimplePanel panel = new ResizableSimplePanel(table);
        panel.setStyleName("gridResizePanel");
        setupFillParent(panel.getElement(), table.getElement());

        gridView = layoutImpl.createGridView(grid, panel);
    }

    public int getHeaderHeight() {
        return grid.headerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        grid.headerHeight = headerHeight;
    }

    public GGridTable getTable() {
        return table;
    }

    public GPropertyDraw getCurrentProperty() {
        return table.getCurrentProperty();
    }
    
    public boolean containsProperty(GPropertyDraw property) {
        return table.containsProperty(property);
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
        formLayout.add(grid, gridView, new DefaultFocusReceiver() {
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
        });
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

    public void modifyGridObject(GGroupObjectValue key, boolean add, int position) {
        table.modifyGroupObject(key, add, position);
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
}
