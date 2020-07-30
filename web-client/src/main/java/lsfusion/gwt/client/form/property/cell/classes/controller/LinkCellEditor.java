package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LinkCellEditor extends StringCellEditor {
    public LinkCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, true, 1000);
    }
}