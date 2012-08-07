package platform.gwt.view2.grid.editor;

import com.google.gwt.dom.client.Element;
import platform.gwt.view2.grid.EditManager;

public class IntegerGridEditor extends TextFieldGridEditor {
    public IntegerGridEditor(EditManager editManager, Object oldValue) {
        super(editManager, oldValue);
    }

    @Override
    protected Integer getCurrentValue(Element parent) {
        String value = getInputElement(parent).getValue();
        return value.isEmpty() ? null : Integer.parseInt(value);
    }
}
