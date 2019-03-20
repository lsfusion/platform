package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;

import javax.swing.*;

public class LogicalPropertyRenderer extends PropertyRenderer {
    private JCheckBox checkBox;
    
    public LogicalPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        getComponent().setHorizontalAlignment(JCheckBox.CENTER);
        getComponent().setBorderPainted(true);
        getComponent().setOpaque(true);
    }

    public JCheckBox getComponent() {
        if (checkBox == null) {
            checkBox = new JCheckBox();
        }
        return checkBox;
    }

    public void setValue(Object value) {
        super.setValue(value);
        getComponent().setSelected(value != null);
    }
}
