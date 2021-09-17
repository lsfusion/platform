package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.form.property.cell.controller.EditManager;

// the cell editor that handles ENTER, ESCAPE, BLUR
public abstract class RequestReplaceValueCellEditor extends ARequestValueCellEditor implements RequestReplaceCellEditor {

    public RequestReplaceValueCellEditor(EditManager editManager) {
        super(editManager);
    }
}
