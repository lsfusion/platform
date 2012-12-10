package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DateTimeGridEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

public class GDateTimeType extends GDataType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridRenderer(GwtSharedUtils.getDefaultDateTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeGridEditor(editManager);
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001 00:00:00";
    }
}
