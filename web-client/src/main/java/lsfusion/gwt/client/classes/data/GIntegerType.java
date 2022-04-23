package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntegerCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;

import java.text.ParseException;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new IntegerCellEditor(editManager, editProperty);
    }

    @Override
    protected Object convertDouble(Double doubleValue) {
        return doubleValue.intValue();
    }

    @Override
    protected int getPrecision() {
        return 8;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeIntegerCaption();
    }

    @Override
    public boolean isId() {
        return true;
    }
}
