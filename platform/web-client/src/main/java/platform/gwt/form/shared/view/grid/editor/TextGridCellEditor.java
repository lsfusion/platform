package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.NativeEditEvent;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class TextGridCellEditor extends AbstractGridCellEditor {
    protected final class ParseException extends Exception {
    }

    public TextGridCellEditor(EditManager editManager) {
        this(editManager, null);
    }

    public TextGridCellEditor(EditManager editManager, Style.TextAlign textAlign) {
        this.textAlign = textAlign == Style.TextAlign.LEFT ? null : textAlign;
        this.editManager = editManager;
    }

    protected EditManager editManager;
    protected Style.TextAlign textAlign;
    protected String currentText = "";

    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        currentText = oldValue == null ? "" : oldValue.toString();
        InputElement inputElement = getInputElement(parent);
        boolean selectAll = true;
        if (editEvent instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) editEvent).getNativeEvent();
            String eventType = nativeEvent.getType();
            if (KEYDOWN.equals(eventType) && nativeEvent.getKeyCode() == KeyCodes.KEY_DELETE) {
                currentText = "";
                selectAll = false;
            } else if (KEYPRESS.equals(eventType)) {
                currentText = String.valueOf((char)nativeEvent.getCharCode());
                selectAll = false;
            }
        }
        inputElement.setValue(currentText);
        inputElement.focus();

        if (selectAll) {
            textBoxImpl.setSelectionRange((com.google.gwt.user.client.Element) (Element) inputElement, 0, currentText.length());
        } else {
            //перемещаем курсор в конец текста
            textBoxImpl.setSelectionRange((com.google.gwt.user.client.Element) (Element) inputElement, currentText.length(), 0);
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
                stopPropagation(event);
                validateAndCommit(parent);
            } else if (keyDown && keyCode == KeyCodes.KEY_ESCAPE) {
                stopPropagation(event);
                editManager.cancelEditing();
            } else {
                currentText = getCurrentText(parent);
            }
        } else if (BLUR.equals(type)) {
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
    public void renderDom(Cell.Context context, DivElement cellParent, Object value) {
        InputElement input = Document.get().createTextInputElement();
        input.setTabIndex(-1);
        input.setValue(currentText);

        Style inputStyle = input.getStyle();
        inputStyle.setBorderWidth(0, Style.Unit.PX);
        inputStyle.setMargin(0, Style.Unit.PX);
        inputStyle.setWidth(100, Style.Unit.PCT);
        inputStyle.setFontSize(8, Style.Unit.PT);
        if (textAlign != null) {
            inputStyle.setTextAlign(textAlign);
        }

        cellParent.appendChild(input);
    }

    private void validateAndCommit(Element parent) {
        String value = getCurrentText(parent);
        try {
            editManager.commitEditing(tryParseInputText(value));
        } catch (ParseException ignore) {
            //если выкинулся ParseException, то не заканчиваем редактирование
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
