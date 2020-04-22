package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.LongGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;

import java.text.ParseException;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LongGridCellEditor(editManager, editProperty);
    }

    @Override
    public Long parseString(String s, String pattern) throws ParseException {
        return parseToDouble(s, pattern).longValue();
    }

    @Override
    protected int getPrecision() {
        return 10;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeLongCaption();
    }
}
