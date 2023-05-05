package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.ValueBoxBase;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class TextCellEditor extends TextBasedCellEditor {

    public TextCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, EditContext editContext) {
        super(editManager, property, inputList, editContext);
    }

    @Override
    protected boolean disableSuggest() {
        return true;
    }

    @Override
    protected Element createTextInputElement() {
        return Document.get().createTextAreaElement();
    }

    @Override
    protected ValueBoxBase<String> createTextBoxBase() {
        return new TextArea();
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
