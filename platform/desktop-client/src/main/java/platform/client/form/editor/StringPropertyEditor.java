package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;

import static platform.client.ClientResourceBundle.getString;

public class StringPropertyEditor extends TextFieldPropertyEditor {

    private final ClientPropertyDraw property;
    private final boolean matchRegexp;

    private String currentError = null;

    public StringPropertyEditor(ClientPropertyDraw property, Object value, final int length, boolean matchRegexp) {
        super(property.design);
        this.matchRegexp = matchRegexp;
        this.property = property;

        setDocument(new PlainDocument() {
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str == null) return;

                if ((getLength() + str.length()) <= length) {
                    super.insertString(offset, str, attr);
                }
            }
        });

        if (value != null) {
            setText(BaseUtils.rtrim(value.toString()));
        }
    }

    public Object getCellEditorValue() {
        String text = getText();
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

    private void showErrorTooltip() {
        currentError = property.regexpMessage == null
                       ? getString("form.editor.incorrect.value")
                       : property.regexpMessage;

        setToolTipText(currentError);

        showToolTip();

        setToolTipText(null);
    }

    private void showToolTip() {
        Action action = getActionMap().get("postTip");
        if (action != null) {
            action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "postTip"));
        }
    }
}