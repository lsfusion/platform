package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import javax.swing.*;

public class FilePropertyRenderer extends LabelPropertyRenderer {
    private String extension;

    public FilePropertyRenderer(ClientPropertyDraw property) {
        this(property, null);
    }

    public FilePropertyRenderer(ClientPropertyDraw property, String extension) {
        super(property);
        this.extension = extension;
        getComponent().setVerticalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setText("");
            getComponent().setIcon(extension != null ? SwingUtils.getSystemIcon(extension) : null);
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
