package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.classes.data.GLongType;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LongGridCellEditor extends IntegralGridCellEditor {
    public LongGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GLongType.instance, editManager, property);
    }
}
