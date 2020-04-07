package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.AbstractGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.NativeEditEvent;

import java.text.ParseException;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.CELL_HORIZONTAL_PADDING;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public abstract class TextBasedGridCellEditor extends AbstractGridCellEditor {
    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final GPropertyDraw property;
    protected final EditManager editManager;
    protected final String inputElementTagName;

    protected String currentText = "";

    public TextBasedGridCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, "input");
    }

    public TextBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String inputElementTagName) {
        this.inputElementTagName = inputElementTagName;
        this.editManager = editManager;
        this.property = property;
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        currentText = renderToString(oldValue);
        InputElement inputElement = getInputElement(parent);
        boolean selectAll = true;
        if (editEvent instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) editEvent).getNativeEvent();
            String eventType = nativeEvent.getType();
            if (KEYDOWN.equals(eventType) && (GKeyStroke.isDeleteKeyEvent(nativeEvent) || GKeyStroke.isBackspaceKeyEvent(nativeEvent))) {
                currentText = "";
                selectAll = false;
            } else if (KEYPRESS.equals(eventType)) {
                int charCode = nativeEvent.getCharCode();
                if (charCode != 0) {
                    String input = String.valueOf((char)charCode);
                    currentText = checkInputValidity(parent, input) ? input : "";
                    selectAll = false;
                }
            }
        }
        inputElement.setValue(currentText);
        inputElement.focus();

        if (selectAll) {
            textBoxImpl.setSelectionRange(inputElement, 0, currentText.length());
        } else {
            //перемещаем курсор в конец текста
            textBoxImpl.setSelectionRange(inputElement, currentText.length(), 0);
        }
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event) {
        String type = event.getType();
        boolean keyDown = KEYDOWN.equals(type);
        boolean keyPress = KEYPRESS.equals(type);
        if (keyDown || keyPress) {
            int keyCode = event.getKeyCode();
            if (keyPress && keyCode == KeyCodes.KEY_ENTER) {
                enterPressed(event, parent);
            } else if (keyDown && keyCode == KeyCodes.KEY_ESCAPE) {
                stopPropagation(event);
                editManager.cancelEditing();
            } else if (keyDown && keyCode == KeyCodes.KEY_DOWN) {
                arrowPressed(event, parent, true);
            } else if (keyDown && keyCode == KeyCodes.KEY_UP) {
                arrowPressed(event, parent, false);
            } else {
                if (GKeyStroke.isCommonEditKeyEvent(event) && (!GKeyStroke.isDeleteKeyEvent(event) || keyPress) && !GKeyStroke.isBackspaceKeyEvent(event) &&
                        !checkInputValidity(parent, String.valueOf((char) event.getCharCode()))) {
                    stopPropagation(event);
                } else {
                    currentText = getCurrentText(parent);
                }
            }
        } else if (BLUR.equals(type)) {
            // Cancel the change. Ensure that we are blurring the input element and
            // not the parent element itself.
            EventTarget eventTarget = event.getEventTarget();
            if (Element.is(eventTarget)) {
                Element target = Element.as(eventTarget);
                if (inputElementTagName.equals(target.getTagName().toLowerCase())) {
                    validateAndCommit(parent, true);
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

    protected void enterPressed(NativeEvent event, Element parent) {
        stopPropagation(event);
        validateAndCommit(parent, false);
    }

    protected void arrowPressed(NativeEvent event, Element parent, boolean down) {
        stopPropagation(event);
        commitAndChangeRow(parent, down);
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, final DivElement cellParent, Object value) {
        final InputElement input = Document.get().createTextInputElement();
        
        Event.sinkEvents(input, Event.ONPASTE);
        Event.setEventListener(input, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (event.getTypeInt() == Event.ONPASTE) {
                    if (!checkInputValidity(cellParent, CopyPasteUtils.getClipboardData(event))) {
                        stopPropagation(event);
                    }
                }
            }
        });
        
        input.setTabIndex(-1);
        input.setValue(currentText);
        input.addClassName("boxSized");
        input.addClassName("textBasedGridCellEditor");

        Style inputStyle = input.getStyle();
        inputStyle.setBorderWidth(0, Style.Unit.PX);
        inputStyle.setOutlineStyle(Style.OutlineStyle.NONE);
        inputStyle.setMargin(0, Style.Unit.PX);
        inputStyle.setPaddingTop(0, Style.Unit.PX);
        inputStyle.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        inputStyle.setPaddingBottom(0, Style.Unit.PX);
        inputStyle.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        inputStyle.setWidth(100, Style.Unit.PCT);
        inputStyle.setHeight(100, Style.Unit.PCT);

        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(inputStyle);
        }
        if (font == null || font.size == 0) {
            inputStyle.setFontSize(8, Style.Unit.PT);
        }
        cellParent.getStyle().setProperty("height", cellParent.getParentElement().getStyle().getHeight());
        cellParent.getStyle().setPadding(0, Style.Unit.PX);

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            inputStyle.setTextAlign(textAlignStyle);
        }

        cellParent.appendChild(input);
    }

    public void validateAndCommit(Element parent, boolean cancelIfInvalid) {
        String value = getCurrentText(parent);
        try {
            editManager.commitEditing(tryParseInputText(value, true));
        } catch (ParseException ignore) {
            //если выкинулся ParseException и фокус ещё в эдиторе, то не заканчиваем редактирование
            if (cancelIfInvalid) {
                editManager.cancelEditing();
            }
        }
    }

    private void commitAndChangeRow(Element parent, boolean moveDown) {
        try {
            String value = getCurrentText(parent);
            editManager.commitEditing(tryParseInputText(value, true));
            editManager.selectNextRow(moveDown);
        } catch (ParseException ignore) {
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
