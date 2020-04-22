package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntegerGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;

import java.text.ParseException;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridCellEditor(editManager, editProperty);
    }

    @Override
    public Integer parseString(String s, String pattern) throws ParseException {
        return parseToDouble(s, pattern).intValue();
    }

    @Override
    protected int getPrecision() {
        return 8;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeIntegerCaption();
    }
}
