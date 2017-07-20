package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.IntegerGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.NumberGridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.form.shared.view.filter.GCompare.*;

//import lsfusion.gwt.form.shared.view.filter.GCompare;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font, String pattern) {
        return 50;
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font, String pattern) {
        return getPreferredPixelWidth(maximumCharWidth, font, pattern);
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font, String pattern) {
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
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Object class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return "Объект";
    }
}
