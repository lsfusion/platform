package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.FileGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.FileGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;
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
    public Object parseString(String s) throws ParseException {
        throw new ParseException("File class doesn't support conversion from string", 0);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new FileGridCellEditor(editManager, editProperty, description, multiple, extensions);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new FileGridCellRenderer();
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 18;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 18;
    }
}
