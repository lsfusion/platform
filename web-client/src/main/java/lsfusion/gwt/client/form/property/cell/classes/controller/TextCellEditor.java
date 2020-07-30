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
        return Document.get().createTextAreaElement();
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
