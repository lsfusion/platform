package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.text.ParseException;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.view.StyleDefaults.DEFAULT_FONT_PT_SIZE;

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
        String text = renderToString(oldValue);
        InputElement inputElement = getInputElement(parent);
        boolean selectAll = true;
        if (GKeyStroke.isCharDeleteKeyEvent(event)) {
            text = "";
            selectAll = false;
        } else if (GKeyStroke.isCharAddKeyEvent(event)) {
            String input = String.valueOf((char) event.getCharCode());
            text = checkInputValidity(parent, input) ? input : "";
            selectAll = false;
        }
        inputElement.setValue(text);
        inputElement.focus();

        if (selectAll) {
            textBoxImpl.setSelectionRange(inputElement, 0, text.length());
        } else {
            //перемещаем курсор в конец текста
            textBoxImpl.setSelectionRange(inputElement, text.length(), 0);
        }
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;

        String type = event.getType();
        if (GKeyStroke.isCharInputKeyEvent(event) || GKeyStroke.isCharDeleteKeyEvent(event) ||
                GKeyStroke.isCharNavigateKeyEvent(event) || GMouseStroke.isEvent(event)) {
            if(GKeyStroke.isCharInputKeyEvent(event) && !checkInputValidity(parent, String.valueOf((char) event.getCharCode())))
                handler.consume(); // this thing is needed to disable inputting incorrect symbols
            else
                handler.consume(true);
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
        String currentValue = input.getValue();
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

    protected String renderToString(Object value) {
        return value == null ? "" : value.toString();
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
    public void renderDom(final Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        final InputElement input = Document.get().createTextInputElement();
        
        input.setTabIndex(-1);
        input.addClassName("boxSized");
        input.addClassName("textBasedGridCellEditor");

        Style inputStyle = input.getStyle();
        inputStyle.setWidth(100, Style.Unit.PCT);
        inputStyle.setHeight(100, Style.Unit.PCT);

        TextBasedCellRenderer.setPadding(inputStyle, false);
        setBaseTextFonts(inputStyle, updateContext);

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            inputStyle.setTextAlign(textAlignStyle);
        }

        cellParent.getStyle().setPadding(0, Style.Unit.PX);
        cellParent.appendChild(input);
    }

    protected void setBaseTextFonts(Style textareaStyle, UpdateContext updateContext) {
        TextBasedCellRenderer.setBasedTextFonts(property, textareaStyle, updateContext);
//        textareaStyle.setFontSize(DEFAULT_FONT_PT_SIZE, Style.Unit.PT);
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

    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().cast();
    }

    private String getCurrentText(Element parent) {
        return getInputElement(parent).getValue();
    }

    protected abstract Object tryParseInputText(String inputText, boolean onCommit) throws ParseException;
}
