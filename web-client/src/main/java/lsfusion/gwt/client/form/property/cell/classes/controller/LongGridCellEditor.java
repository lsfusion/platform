package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.GLongType;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LongGridCellEditor extends IntegralGridCellEditor {
    public LongGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GLongType.instance, editManager, property);
    }
}
