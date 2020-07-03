package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class IntegerCellEditor extends IntegralCellEditor {
    public IntegerCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }
}
