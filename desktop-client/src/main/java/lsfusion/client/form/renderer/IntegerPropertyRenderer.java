package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class IntegerPropertyRenderer extends LabelPropertyRenderer {

    public IntegerPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setHorizontalAlignment(JLabel.RIGHT);
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null || property == null || !property.isEditableNotNull()) {
            getComponent().setText(value == null ? "" : format.format(value));
        }
    }
}
