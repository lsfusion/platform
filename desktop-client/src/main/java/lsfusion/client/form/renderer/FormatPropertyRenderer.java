package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.text.Format;

public abstract class FormatPropertyRenderer extends LabelPropertyRenderer {

    protected Format format;

    public FormatPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        updateFormat();
        getComponent().setHorizontalAlignment(JLabel.RIGHT);
    }
    
    public void updateFormat() {
        this.format = property.getFormat();
    }

    protected Object preformat(Object value) {
        return value;
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null || !property.isEditableNotNull()) {
            getComponent().setText(value == null ? "" : format.format(preformat(value)));
        }
    }

}
