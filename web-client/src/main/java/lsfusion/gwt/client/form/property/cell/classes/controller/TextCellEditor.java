package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class TextCellEditor extends TextBasedCellEditor {

    public TextCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, EditContext editContext) {
        super(editManager, property, inputList, null, editContext);
    }

    @Override
    protected boolean isMultiLine() {
        return true;
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }
}
