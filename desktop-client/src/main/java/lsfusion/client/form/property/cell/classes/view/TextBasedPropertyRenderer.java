package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import java.awt.*;

public class TextBasedPropertyRenderer extends LabelPropertyRenderer {
    private boolean isEditableNotNull;

    protected TextBasedPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        isEditableNotNull = value == null && property != null && property.isEditableNotNull();
        if (isEditableNotNull) {
            getComponent().setText(getRequiredStringValue());
        }
    }

    @Override
    public void paintLabelComponent(Graphics g) {
        super.paintLabelComponent(g);
        if(isEditableNotNull) {
            SwingUtils.drawHorizontalLine((Graphics2D) g, Color.RED, 0, getComponent().getWidth(), getComponent().getHeight() - 5);
        }
    }
}