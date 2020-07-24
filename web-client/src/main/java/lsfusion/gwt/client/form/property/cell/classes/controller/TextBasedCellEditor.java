package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public abstract class TextBasedCellEditor implements ReplaceCellEditor {
    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final GPropertyDraw property;
    protected final EditManager editManager;
    protected final String inputElementTagName;

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, "input");
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, String inputElementTagName) {
        this.inputElementTagName = inputElementTagName;
        this.editManager = editManager;
        this.property = property;
    }

    @Override
    public void startEditing(Event event, Element parent, Object oldValue) {
        String value = tryFormatInputText(oldValue);
        InputElement inputElement = getInputElement(parent);
        boolean selectAll = true;
        if (GKeyStroke.isCharDeleteKeyEvent(event)) {
            value = "";
            selectAll = false;
        } else if (GKeyStroke.isCharAddKeyEvent(event)) {
            String input = String.valueOf((char) event.getCharCode());
            value = checkInputValidity(parent, input) ? input : "";
            selectAll = false;
        }
        //we need this order (focus before setValue) for single click editing IntegralCellEditor (type=number)
        inputElement.focus();
        setValue(inputElement, value);

        if (selectAll) {
            inputElement.select();
        }
    }

    private native void setValue(Element element, Object value) /*-{
        element.value = value;
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;

        String type = event.getType();
        if (GKeyStroke.isCharAddKeyEvent(event) || GKeyStroke.isCharDeleteKeyEvent(event) ||
                GKeyStroke.isCharNavigateKeyEvent(event) || GMouseStroke.isEvent(event) || GKeyStroke.isPasteFromClipboardEvent(event) || GMouseStroke.isContextMenuEvent(event)) {
            boolean isCorrect = true;

            String stringToAdd = null;
            if(GKeyStroke.isCharAddKeyEvent(event))
                stringToAdd = String.valueOf((char) event.getCharCode());
            else if(GKeyStroke.isPasteFromClipboardEvent(event))
                stringToAdd = CopyPasteUtils.getEventClipboardData(event);
            if(stringToAdd != null && !checkInputValidity(parent, stringToAdd))
                isCorrect = false; // this thing is needed to disable inputting incorrect symbols

            handler.consume(isCorrect);
        } else if (KEYDOWN.equals(type)) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyCodes.KEY_ENTER) {
                enterPressed(handler, parent);
            } else if (keyCode == KeyCodes.KEY_ESCAPE) {
                escapePressed(handler, parent);
            }
//                else
//                if (keyCode == KeyCodes.KEY_DOWN) {
//                    handler.consume();
//                    arrowPressed(event, parent, true);
//                } else if (keyCode == KeyCodes.KEY_UP) {
//                    handler.consume();
//                    arrowPressed(event, parent, false);
//                }
        } else if (BLUR.equals(type)) {
            // Cancel the change. Ensure that we are blurring the input element and
            // not the parent element itself.
            EventTarget eventTarget = event.getEventTarget();
            if (Element.is(eventTarget)) {
                Element target = Element.as(eventTarget);
                if (inputElementTagName.equals(target.getTagName().toLowerCase())) {
                    validateAndCommit(parent, true, true);
                }
            }
        }
    }
    
    private boolean checkInputValidity(Element parent, String stringToAdd) {
        InputElement input = getInputElement(parent);
        int cursorPosition = textBoxImpl.getCursorPos(input);
        int selectionLength = textBoxImpl.getSelectionLength(input);
        String currentValue = getCurrentText(parent);
        String firstPart = currentValue == null ? "" : currentValue.substring(0, cursorPosition);
        String secondPart = currentValue == null ? "" : currentValue.substring(cursorPosition + selectionLength);
        
        return isStringValid(firstPart + stringToAdd + secondPart);
    }
    
    protected boolean isStringValid(String string) {
        try {
            tryParseInputText(string, false);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    protected boolean checkEnterEvent(Event event) {
        return GKeyStroke.isPlainKeyEvent(event);
    }
    protected void enterPressed(EventHandler handler, Element parent) {
        if(checkEnterEvent(handler.event)) {
            handler.consume();
            validateAndCommit(parent, false, false);
        }
    }
    protected void escapePressed(EventHandler handler, Element parent) {
        if(GKeyStroke.isPlainKeyEvent(handler.event)) {
            handler.consume();
            editManager.cancelEditing();
        }
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext) {
        Element inputElement = createInputElement();
        // without setting boxSized class textarea and input behaviour is pretty odd when text is very large or inside td (position of textarea / input is really unpredictable)
        inputElement.addClassName("boxSized");

        Style.TextAlign textAlign = property.getTextAlignStyle();
        if(textAlign != null)
            inputElement.getStyle().setTextAlign(textAlign);

        inputElement.getStyle().setHeight(100, Style.Unit.PCT);
        inputElement.getStyle().setWidth(100, Style.Unit.PCT); // input doesn't respect justify-content, stretch, plus we want to include paddings in input (to avoid having "selection border")

        TextBasedCellRenderer.render(property, inputElement, renderContext, isMultiLine(), false);

        cellParent.appendChild(inputElement);
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(cellParent);
    }

    protected boolean isMultiLine() {
        return false;
    }

    public void commitEditing(Element parent) {
        validateAndCommit(parent, true, false);
    }

    public void validateAndCommit(Element parent, boolean cancelIfInvalid, boolean blurred) {
        String value = getCurrentText(parent);
        try {
            editManager.commitEditing(tryParseInputText(value, true), blurred);
        } catch (ParseException ignore) {
            if (cancelIfInvalid) {
                editManager.cancelEditing();
            }
        }
    }

    public Element createInputElement() {
        return Document.get().createTextInputElement();
    }

    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().cast();
    }

    private String getCurrentText(Element parent) {
        return getInputElement(parent).getValue();
    }

    protected abstract Object tryParseInputText(String inputText, boolean onCommit) throws ParseException;

    protected String tryFormatInputText(Object value) {
        return value == null ? "" : value.toString();
    }
}
