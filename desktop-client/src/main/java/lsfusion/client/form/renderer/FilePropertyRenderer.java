package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public abstract class FilePropertyRenderer extends LabelPropertyRenderer {

    public FilePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setText(null);
        } else {
            setIcon(null);
            if (property.isEditableNotNull()) {
                setText(REQUIRED_STRING);
                setForeground(REQUIRED_FOREGROUND);
            }
        }
        setSelected(isSelected, hasFocus);
    }
}
