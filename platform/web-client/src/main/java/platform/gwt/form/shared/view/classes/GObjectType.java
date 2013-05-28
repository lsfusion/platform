package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.IntegerGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.NumberGridCellRenderer;

import java.text.ParseException;

import static platform.gwt.form.shared.view.filter.GCompare.*;

//import platform.gwt.form.shared.view.filter.GCompare;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 50;
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        return getPreferredPixelWidth(maximumCharWidth, font);
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 50;
    }

    @Override
    public GridCellEditor createValueCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridCellEditor(editManager, editProperty);
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Object class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return "Объект";
    }
}
