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

    private final boolean rich;

    public TextCellEditor(EditManager editManager, GPropertyDraw property, boolean rich) {
        super(editManager, property, "textarea");
        this.rich = rich;
    }

    @Override
    public Element createInputElement() {
        TextAreaElement input = Document.get().createTextAreaElement();
        if (!rich) {
            input.getStyle().setProperty("wordWrap", "break-word");
        }
        return input;
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
