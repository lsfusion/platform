package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.awt.*;

public class StringPropertyRenderer extends LabelPropertyRenderer {

    private final boolean echoSymbols;

    public StringPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        echoSymbols = property != null && property.echoSymbols;
    }

    @Override
    protected void drawForeground(Color conditionalForeground) {
        if (value == null) {
            if (property != null && property.isEditableNotNull()) {
                getComponent().setForeground(REQUIRED_FOREGROUND);
            } else {
                getComponent().setForeground(INACTIVE_FOREGROUND);
            }
        } else {
            getComponent().setForeground(conditionalForeground != null ? conditionalForeground : defaultForeground);
        }
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null) {
            getComponent().setText(echoSymbols ? "******" : value.toString());
        } else if (property == null || !property.isEditableNotNull()) {
            getComponent().setText(EMPTY_STRING);
        }
    }
}
