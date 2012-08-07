package platform.gwt.view2.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import platform.gwt.view2.grid.EditManager;

public class DoubleGridEditor extends TextFieldGridEditor {
    public DoubleGridEditor(EditManager editManager, Object oldValue) {
        super(editManager, oldValue);
        NumberFormat f = NumberFormat.getDecimalFormat();
    }

    protected Double getCurrentValue(Element parent) {
        String value = getInputElement(parent).getValue();
        return value.isEmpty() ? null : Double.parseDouble(value);
    }
}