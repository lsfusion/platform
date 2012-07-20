package platform.gwt.view2.classes;

import com.google.gwt.user.cellview.client.Column;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.GridDataRecord;
import platform.gwt.view2.grid.EditManager;
import platform.gwt.view2.grid.GridEditableCell;
import platform.gwt.view2.grid.editor.GridCellEditor;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.StringGridRenderer;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.panel.StringPanelRenderer;

import java.io.Serializable;

public abstract class GType implements Serializable {
    public Object parseString(String strValue) {
        return strValue;
    }

    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new StringPanelRenderer(property);
    }

    public Column<GridDataRecord, Object> createGridColumn(EditManager editManager, final FormLogicsProvider form, final GPropertyDraw property) {
        return new Column<GridDataRecord, Object>(new GridEditableCell(editManager, createGridCellRenderer())) {
            @Override
            public Object getValue(GridDataRecord record) {
                return record.getAttribute(property.sID);
            }
        };
    }

    public GridCellRenderer createGridCellRenderer() {
        return new StringGridRenderer();
    }

    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, Object oldValue) {
        return null;
    }
}
