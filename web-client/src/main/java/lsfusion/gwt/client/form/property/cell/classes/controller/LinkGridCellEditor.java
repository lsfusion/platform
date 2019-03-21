package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class LinkGridCellEditor extends StringGridCellEditor {
    public LinkGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, true, 1000);
    }
}