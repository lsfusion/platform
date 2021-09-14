package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import javax.swing.*;

public abstract class FilePropertyRenderer extends LabelPropertyRenderer {

    public FilePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setText("");
        } else {
            getComponent().setIcon(null);
        }
        super.setValue(value);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
