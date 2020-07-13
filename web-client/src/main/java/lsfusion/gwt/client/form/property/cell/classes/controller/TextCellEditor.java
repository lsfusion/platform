package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.*;

public class TextCellEditor extends TextBasedCellEditor {

    public TextCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, "textarea");
    }

    @Override
    public Element createInputElement() {
        TextAreaElement inputElement = Document.get().createTextAreaElement();
        // without setting boxSized class textarea behaviour is pretty odd when text is very large or inside td (position of textarea is really unpredictable)
        // maybe for regular input it also makes sense, but so far it's not evident
        inputElement.addClassName("boxSized");
        return inputElement;
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
