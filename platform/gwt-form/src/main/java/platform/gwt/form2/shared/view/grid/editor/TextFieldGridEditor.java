package platform.gwt.form2.shared.view.grid.editor;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import platform.gwt.form2.shared.view.grid.EditManager;

public abstract class TextFieldGridEditor implements GridCellEditor {
    interface Template extends SafeHtmlTemplates {
        @Template("<input style=\"border: 0px; margin: 0px; width: 100%; \" type=\"text\" value=\"{0}\" tabindex=\"-1\"></input>")
        SafeHtml input(String value);
    }

    protected final class ParseException extends Exception {
    }

    protected static Template template;
    public static void initTemplateIfNeeded() {
        if (template == null) {
            template = GWT.create(Template.class);
        }
    }

    public TextFieldGridEditor(EditManager editManager) {
        initTemplateIfNeeded();
        this.editManager = editManager;
    }

    protected EditManager editManager;
    protected String currentText = "";

    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    @Override
    public void startEditing(NativeEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        currentText = oldValue == null ? "" : oldValue.toString();
        InputElement inputElement = getInputElement(parent);
        if (editEvent != null) {
            boolean charEvent = "keypress".equals(editEvent.getType());
            if (charEvent) {
                currentText = String.valueOf((char)editEvent.getCharCode());
            }
        }
        inputElement.setValue(currentText);
        //перемещаем курсор в конец текста
        textBoxImpl.setSelectionRange((com.google.gwt.user.client.Element) (Element) inputElement, currentText.length(), 0);
        inputElement.focus();
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event, ValueUpdater<Object> valueUpdater) {
        String type = event.getType();
        boolean keyUp = "keyup".equals(type);
        boolean keyDown = "keydown".equals(type);
        boolean keyPress = "keypress".equals(type);
        if (keyUp || keyDown || keyPress) {
            int keyCode = event.getKeyCode();
            if (keyPress && keyCode == KeyCodes.KEY_ENTER) {
                validateAndCommit(parent);
            } else if (keyUp && keyCode == KeyCodes.KEY_ESCAPE) {
                editManager.cancelEditing();
            } else {
                currentText = getCurrentText(parent);
            }
        } else if ("blur".equals(type)) {
            // Cancel the change. Ensure that we are blurring the input element and
            // not the parent element itself.
            EventTarget eventTarget = event.getEventTarget();
            if (Element.is(eventTarget)) {
                Element target = Element.as(eventTarget);
                if ("input".equals(target.getTagName().toLowerCase())) {
                    editManager.cancelEditing();
                }
            }
        }
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        sb.append(template.input(currentText));
    }

    @Override
    public boolean resetFocus(Cell.Context context, Element parent, Object value) {
        getInputElement(parent).focus();
        return true;
    }

    private void validateAndCommit(Element parent) {
        String value = getCurrentText(parent);
        try {
            editManager.commitEditing(tryParseInputText(value));
        } catch (ParseException ignore) {
        }
    }

    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().cast();
    }

    private String getCurrentText(Element parent) {
        return getInputElement(parent).getValue();
    }

    protected abstract Object tryParseInputText(String inputText) throws ParseException;
}
