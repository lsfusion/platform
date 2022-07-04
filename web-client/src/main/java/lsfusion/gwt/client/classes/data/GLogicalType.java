package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.LogicalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.LogicalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

public class GLogicalType extends GDataType {
    public static GLogicalType instance = new GLogicalType(false);

    public static GLogicalType threeStateInstance = new GLogicalType(true);

    public boolean threeState;

    public GLogicalType() {
    }

    public GLogicalType(boolean threeState) {
        this.threeState = threeState;
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new LogicalCellRenderer(property, threeState);
}

    @Override
    public CellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new LogicalCellEditor(editManager, threeState);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if(needNotNull)
            return GSize.CONST(30);

        return null;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        try {
            return GwtSharedUtils.nullBoolean(Boolean.parseBoolean(s));
        } catch (NumberFormatException nfe) {
            throw new ParseException("string " + s + "can not be converted to logical", 0);
        }
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeLogicalCaption();
    }
}
