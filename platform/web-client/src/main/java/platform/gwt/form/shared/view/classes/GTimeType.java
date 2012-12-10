package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TimeGridEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridRenderer(GwtSharedUtils.getDefaultTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TimeGridEditor(editManager);
    }

    @Override
    public String getPreferredMask() {
        return "00:00:00";
    }
}
