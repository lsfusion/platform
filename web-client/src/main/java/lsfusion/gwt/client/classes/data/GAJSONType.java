package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

public abstract class GAJSONType extends GDataType {

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, EditContext editContext) {
        return new TextCellEditor(editManager, editProperty, inputList, editContext);
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw) {
        return GSize.CONST(150);
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }
}
