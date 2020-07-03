package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.LinkCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FileCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.form.filter.user.GCompare.EQUALS;
import static lsfusion.gwt.client.form.filter.user.GCompare.NOT_EQUALS;

public abstract class GLinkType extends GDataType {
    public boolean multiple;
    public String description;

    public GLinkType() {
    }

    public GLinkType(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LinkCellEditor(editManager, editProperty);
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