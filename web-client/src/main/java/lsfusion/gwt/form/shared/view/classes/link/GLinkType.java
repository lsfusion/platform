package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GDataType;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.LinkGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.LinkGridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.form.shared.view.filter.GCompare.EQUALS;
import static lsfusion.gwt.form.shared.view.filter.GCompare.NOT_EQUALS;

public abstract class GLinkType extends GDataType {
    public boolean multiple;
    public String description;

    public GLinkType() {
    }

    public GLinkType(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public String getPreferredMask(String pattern) {
        return "1234567";
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("File class doesn't support conversion from string", 0);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LinkGridCellEditor(editManager, editProperty);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new LinkGridCellRenderer(property);
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font, String pattern) {
        return 18;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font, String pattern) {
        return 18;
    }
}