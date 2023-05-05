package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.LongCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.IntegralCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.form.filter.user.GCompare.*;

//import lsfusion.gwt.shared.form.view.filter.GCompare;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new IntegralCellRenderer(property);
    }

    @Override
    protected int getDefaultCharWidth() {
        return 7;
    }

    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, EditContext editContext) {
        return new LongCellEditor(editManager, editProperty);
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS};
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Object class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeObjectCaption();
    }
}
