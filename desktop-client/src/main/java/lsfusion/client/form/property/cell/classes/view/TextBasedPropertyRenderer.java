package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;
import org.apache.commons.lang3.StringUtils;

public class TextBasedPropertyRenderer extends LabelPropertyRenderer {

    protected TextBasedPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        if (value == null && property != null && property.isEditableNotNull()) {
            getComponent().setText(MainController.showNotDefinedStrings ? REQUIRED_STRING : ("<html><u>" + StringUtils.repeat('_', property.getValueWidth(getComponent())) + "</u></html>"));
        }
    }
}