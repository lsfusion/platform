package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static lsfusion.base.BaseUtils.rtrim;
import static lsfusion.client.ClientResourceBundle.getString;

public class StringPropertyEditor extends TextFieldPropertyEditor {

    private final ClientPropertyDraw property;
    private final boolean matchRegexp;
    private final boolean isVarString;

    private String currentError = null;

    public StringPropertyEditor(ClientPropertyDraw property, Object value, final int length, boolean isVarString, boolean matchRegexp) {
        super(property);
        this.isVarString = isVarString;
        this.matchRegexp = matchRegexp;
        this.property = property;

        setDocument(new PlainDocument() {
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str == null) return;

                if ((getLength() + str.length()) > length) { // если длина больше trim'им, потому как иначе из-за selectAll курсор будет вправо уплывать
                    str = str.substring(0, length - getLength());
                }
                super.insertString(offset, str, attr);
            }
        });

        if (value != null) {
            setText(value.toString());
        }

        setComponentPopupMenu(new EditorContextMenu(this));
    }

    public Object getCellEditorValue() {
        String text = getText();
        if (!isVarString) {
            text = rtrim(text);
        }
        return text.isEmpty() ? null : text;
    }

    @Override
    public boolean stopCellEditing() {
        if (!super.stopCellEditing()) {
            return false;
        }

        if (matchRegexp && property.regexp != null && getText() != null && !getText().isEmpty()) {
            if (!getText().matches(property.regexp)) {
                showErrorTooltip();
                return false;
            }
        }

        return true;
    }

    @Override
    public void cancelCellEditing() { }

    private void showErrorTooltip() {
        currentError = property.regexpMessage == null
                       ? getString("form.editor.incorrect.value")
                       : property.regexpMessage;

        setToolTipText(currentError);

        //имитируем ctrl+F1 http://qaru.site/questions/368838/force-a-java-tooltip-to-appear
        dispatchEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), InputEvent.CTRL_MASK,
                KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED));

        setToolTipText(null);
    }
}