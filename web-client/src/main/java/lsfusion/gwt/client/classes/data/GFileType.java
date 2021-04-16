package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.FileCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FileCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;
import java.util.ArrayList;

import static lsfusion.gwt.client.form.filter.user.GCompare.EQUALS;
import static lsfusion.gwt.client.form.filter.user.GCompare.NOT_EQUALS;

public abstract class GFileType extends GDataType {
    public boolean multiple;
    public boolean storeName;
    public String description;
    public ArrayList<String> validContentTypes;

    public GFileType() {
    }

    public GFileType(boolean multiple, boolean storeName) {
        this.multiple = multiple;
        this.storeName = storeName;
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
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, boolean hasList) {
        return new FileCellEditor(editManager, description, storeName, validContentTypes);
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new FileCellRenderer(property);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw) {
        return 18;
    }

}
