package platform.gwt.view2.grid.editor;

import com.google.gwt.dom.client.Element;
import platform.gwt.view2.grid.EditManager;

public class StringGridEditor extends TextFieldGridEditor {
    public StringGridEditor(EditManager editManager, Object oldValue) {
        super(editManager, oldValue);
    }

    protected String getCurrentValue(Element parent) {
        return getInputElement(parent).getValue();
    }
}
