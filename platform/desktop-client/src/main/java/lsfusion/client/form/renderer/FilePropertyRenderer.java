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
}
