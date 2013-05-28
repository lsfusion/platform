package platform.client.form.editor;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DoubleFilter extends DocumentFilter {
    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
                             String string, AttributeSet attr)
            throws BadLocationException {

        StringBuffer buffer = new StringBuffer(string);
        for (int i = buffer.length() - 1; i >= 0; i--) {
            char ch = buffer.charAt(i);
            if (!Character.isDigit(ch) && !(ch == '-') && !(ch == '.')) {
                if (ch == ',') {
                    buffer.replace(i, i+1, ".");
                } else {
                    buffer.deleteCharAt(i);
                }
            }
        }
        super.insertString(fb, offset, buffer.toString(), attr);
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb,
                        int offset, int length, String string, AttributeSet attr) throws BadLocationException {
        if (length > 0) fb.remove(offset, length);
        insertString(fb, offset, string, attr);
    }
}
