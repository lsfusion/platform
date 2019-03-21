package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.data.GLongType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LongGridCellEditor extends IntegralGridCellEditor {
    public LongGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GLongType.instance, editManager, property);
    }
}
