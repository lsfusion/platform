package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.text.ParseException;

public class StringPropertyEditor extends TextFieldPropertyEditor {

    ClientPropertyDraw property;

    public StringPropertyEditor(final int length, Object value, ClientPropertyDraw property) {
        super(property.design);
        this.property = property;

        setDocument(new PlainDocument() {

            public void insertString(int offset, String  str, AttributeSet attr) throws BadLocationException {

                if (str == null) return;

                if ((getLength() + str.length()) <= length)
                    super.insertString(offset, str, attr);
            }
        });

        if (value != null) {
            setText(BaseUtils.rtrim(value.toString()));
        }
    }

    public Object getCellEditorValue(){

        String text = getText();
        if (text.isEmpty()) return null;
        return text;
    }


    @Override
    public String checkValue(Object value) {
        if (property.regexp != null)
            if (!getText().matches(property.regexp)) {
                return property.regexpMessage==null ?
                        ClientResourceBundle.getString("form.editor.incorrect.value") : property.regexpMessage;
            }
        return null;
        }
}