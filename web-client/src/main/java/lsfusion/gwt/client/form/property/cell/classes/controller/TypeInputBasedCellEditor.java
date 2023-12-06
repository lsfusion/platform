package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;

public abstract class TypeInputBasedCellEditor extends InputBasedCellEditor implements KeepCellEditor {

    public TypeInputBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

}
