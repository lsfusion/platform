package lsfusion.client.form.renderer.link;

import lsfusion.client.form.renderer.LabelPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public abstract class LinkPropertyRenderer extends LabelPropertyRenderer {
    protected String link;

    public LinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        link = (String) value;
        if (value != null) {
            setText(null);
        } else {
            if (property.isEditableNotNull()) {
                setText(REQUIRED_STRING);
                setForeground(REQUIRED_FOREGROUND);
            }
        }
        setSelected(isSelected, hasFocus);
    }
}