package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.LongGridCellEditor;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.NumberGridCellRenderer;
import lsfusion.gwt.shared.view.GFont;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.GWidthStringProcessor;
import lsfusion.gwt.shared.view.filter.GCompare;

import java.text.ParseException;

import static lsfusion.gwt.shared.view.filter.GCompare.*;

//import lsfusion.gwt.shared.form.view.filter.GCompare;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    public int getFullWidthString(String widthString, GFont font, GWidthStringProcessor widthStringProcessor) {
        return GDataType.getFullWidthString(font, widthString, widthStringProcessor);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return getFullWidthString("0000000", font, widthStringProcessor);
    }

    @Override
    public GridCellEditor createValueCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LongGridCellEditor(editManager, editProperty);
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
        return ClientMessages.Instance.get().typeObjectCaption();
    }
}
