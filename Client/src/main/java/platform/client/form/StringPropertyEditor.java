package platform.client.form;

import java.awt.*;

public class StringPropertyEditor extends TextFieldPropertyEditor
                           implements PropertyEditorComponent {

    public StringPropertyEditor(Object value) {
        super();

        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Component getComponent() {
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
