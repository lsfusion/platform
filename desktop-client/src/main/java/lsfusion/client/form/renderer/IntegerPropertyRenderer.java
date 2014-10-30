package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class IntegerPropertyRenderer extends LabelPropertyRenderer {

    public IntegerPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.RIGHT);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value == null && property.isEditableNotNull()) {
            setText(REQUIRED_STRING);
            setForeground(REQUIRED_FOREGROUND);
        } else {
            setText(value == null ? "" : format.format(value));
            setForeground(UIManager.getColor("TextField.foreground"));
        }
        setSelected(isSelected, hasFocus);
    }
}
