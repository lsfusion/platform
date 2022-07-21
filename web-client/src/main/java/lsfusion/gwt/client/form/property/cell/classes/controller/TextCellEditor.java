package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ValueBoxBase;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.TextArea;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class TextCellEditor extends SimpleTextBasedCellEditor {

    public TextCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        super(editManager, property, inputList);
    }

    @Override
    protected boolean disableSuggest() {
        return true;
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
