package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

public class TextBasedPropertyRenderer extends LabelPropertyRenderer {

    protected TextBasedPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        if (value == null && property != null && property.isEditableNotNull()) {
            getComponent().setText(getRequiredStringValue());
        }
    }
}