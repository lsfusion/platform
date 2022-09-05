package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.LinkCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FileCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.form.filter.user.GCompare.EQUALS;

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
        return new GCompare[] {EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public CellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new LinkCellEditor(editManager, editProperty);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new FileCellRenderer(property);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if(needNotNull)
            return GSize.CONST(18);

        return null;
    }

}