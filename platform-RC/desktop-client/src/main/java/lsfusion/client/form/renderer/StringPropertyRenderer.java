package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class StringPropertyRenderer extends LabelPropertyRenderer {

    private final boolean echoSymbols;

    public StringPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        echoSymbols = property != null && property.echoSymbols;

//        setHorizontalAlignment(JLabel.LEFT);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        super.setValue(value, isSelected, hasFocus);

        if (value != null || !property.isEditableNotNull()) {
            if (value != null) {
                setText(echoSymbols ? "******" : value.toString());
            } else {
                setForeground(INACTIVE_FOREGROUND);
                setText(EMPTY_STRING);
            }
        }
    }
}
