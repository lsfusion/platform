package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.FileGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

import java.util.ArrayList;

import static platform.gwt.form.shared.view.filter.GCompare.EQUALS;
import static platform.gwt.form.shared.view.filter.GCompare.NOT_EQUALS;

public abstract class GFileType extends GDataType {
    public boolean multiple;
    public String description;
    public ArrayList<String> extensions;

    public GFileType() {
    }

    public GFileType(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public String getPreferredMask() {
        return "1234567";
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new FileGridCellEditor(editManager, editProperty, description, multiple, extensions);
    }
}
