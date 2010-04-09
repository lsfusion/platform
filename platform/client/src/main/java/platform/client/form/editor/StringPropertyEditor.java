package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.EventObject;

public class StringPropertyEditor extends TextFieldPropertyEditor
                           implements PropertyEditorComponent {

    public StringPropertyEditor(final int length, Object value) {
        super();

        setDocument(new PlainDocument() {

            public void insertString(int offset, String  str, AttributeSet attr) throws BadLocationException {

                if (str == null) return;

                if ((getLength() + str.length()) <= length)
                    super.insertString(offset, str, attr);
            }
        });

        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public Object getCellEditorValue() {
        if (getText().isEmpty()) return null;
        return getText();
    }

    public boolean valueChanged() {
        return true;
    }

}