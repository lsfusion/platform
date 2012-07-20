package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.grid.EditManager;
import platform.gwt.view2.grid.editor.GridCellEditor;
import platform.gwt.view2.grid.editor.StringGridEditor;

public class GStringType extends GDataType {
    protected int length = 50;

    public GStringType() {}

    public GStringType(int length) {
        this.length = length;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, Object oldValue) {
        return new StringGridEditor(editManager, oldValue);
    }
}
