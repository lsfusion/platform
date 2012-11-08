package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import platform.gwt.form.shared.view.GGrid;
import platform.gwt.form.shared.view.GOrder;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.List;
import java.util.Map;

public class GGridController {
    private GGrid grid;
    private CellPanel gridView;
    private GGridTable table;

    public GGridController(GGrid igrid, GFormController iformController, GGroupObjectController igroupObject) {
        grid = igrid;
        gridView = new VerticalPanel();
        table = new GGridTable(iformController, igroupObject);

        ResizeLayoutPanel panel = new ResizeLayoutPanel();
        panel.setStyleName("gridResizePanel");
        panel.setSize("100%", "100%");
        gridView.setSize("100%", "100%");
        panel.setWidget(table);
        gridView.add(panel);

        gridView.setCellHeight(panel, "100%");
    }

    public GGridTable getTable() {
        return table;
    }

    public void rememberScrollPosition() {
        table.rememberScrollPosition();
    }

    public void update() {
        table.update();
    }

    public void preparePendingState() {
        table.preparePendingState();
    }

    public void applyPendingState() {
        table.applyPendingState();
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(grid, gridView);
//        formLayout.setTableCellSize(grid.container, gridView, "100%", false);
    }

    public void hide() {
        gridView.setVisible(false);
    }

    public void show() {
        gridView.setVisible(true);
    }

    public void redrawGrid() {
        table.onResize();
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
}
