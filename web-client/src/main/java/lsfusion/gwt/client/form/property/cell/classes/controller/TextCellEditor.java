package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.ValueBoxBase;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.*;

public class TextCellEditor extends TextBasedCellEditor {

    public TextCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        super(editManager, property, "textarea", inputList);
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
    protected String tryParseInputText(String inputText, boolean onCommit) {
        return (inputText == null || inputText.isEmpty()) ? null : inputText;
    }

    @Override
    protected boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }
}
