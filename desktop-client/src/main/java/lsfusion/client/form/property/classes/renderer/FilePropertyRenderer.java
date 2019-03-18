package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;

public abstract class FilePropertyRenderer extends LabelPropertyRenderer {

    public FilePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setHorizontalAlignment(JLabel.CENTER);
        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setText(null);
        } else {
            getComponent().setIcon(null);
        }
        super.setValue(value);
    }
}
