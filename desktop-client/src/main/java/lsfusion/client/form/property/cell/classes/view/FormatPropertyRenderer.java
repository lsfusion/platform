package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import java.text.Format;

public abstract class FormatPropertyRenderer extends LabelPropertyRenderer {

    protected Format format;

    public FormatPropertyRenderer(ClientPropertyDraw property) {
        super(property);

        updateFormat();
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
