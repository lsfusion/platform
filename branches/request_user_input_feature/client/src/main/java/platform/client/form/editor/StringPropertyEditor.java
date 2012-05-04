package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.event.MouseEvent;

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

        if (matchRegexp && property.regexp != null && getText() != null) {
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

        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(this, 0, 0, 0, 0, 0, 0, false)
        );
    }

    @Override
    public void removeNotify() {
        ToolTipManager.sharedInstance().mousePressed(
                new MouseEvent(this, 0, 0, 0, 0, 0, 0, false)
        );
        super.removeNotify();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return currentError;
    }
}