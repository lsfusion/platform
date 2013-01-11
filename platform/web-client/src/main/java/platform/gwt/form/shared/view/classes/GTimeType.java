package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TimeGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property, GwtSharedUtils.getDefaultTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public String getPreferredMask() {
        return "00:00:00";
    }
}
