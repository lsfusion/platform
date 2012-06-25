package platform.gwt.form.client.form.ui;

import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.view.GGrid;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.Map;

public class GGridController {
    private GGrid key;
    private VLayout gridView;
    private GGridTable table;
    private GFormLayout formLayout;

    public GGridController(GGrid key, GFormController iformController, GGroupObjectController igroupObject, GFormLayout iformLayout) {
        this.key = key;
        formLayout = iformLayout;
        gridView = new VLayout();
        table = new GGridTable(iformController, igroupObject);
        gridView.addMember(table);
        gridView.addMember(igroupObject.getGridToolbar());
    }

    public VLayout getGridView() {
        return gridView;
    }

    public GGridTable getTable() {
        return table;
    }

    public void update() {
        table.update();
    }

    public void addView() {
        formLayout.add(key, gridView);
    }

    public GGrid getGrid() {
        return key;
    }

    public void hide() {
        formLayout.remove(key);
    }

    public void show() {
        formLayout.add(key, gridView, 0);
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
}
