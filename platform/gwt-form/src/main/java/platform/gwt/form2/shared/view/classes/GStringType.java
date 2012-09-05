package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.editor.StringGridEditor;

public class GStringType extends GDataType {
    protected int length = 50;

    public GStringType() {}

    public GStringType(int length) {
        this.length = length;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridEditor(editManager);
    }
}
