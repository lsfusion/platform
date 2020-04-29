package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.ClientPropertyDraw;

public class StringPropertyRenderer extends TextBasedPropertyRenderer {

    private final boolean echoSymbols;

    public StringPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        echoSymbols = property != null && property.echoSymbols;
    }

    @Override
    protected boolean showNotDefinedString() {
        return true;
    }

    public void setValue(Object value) {
        super.setValue(value != null && value.toString().isEmpty() && !MainController.showNotDefinedStrings ? null : value);

        if (value != null) {
            getComponent().setText(echoSymbols ? "******" : (value.toString().isEmpty() && !MainController.showNotDefinedStrings ? EMPTY_STRING : value.toString()));
        } else if (property == null || !property.isEditableNotNull()) {
            getComponent().setText(MainController.showNotDefinedStrings ? NOT_DEFINED_STRING : "");
        }
    }
}
