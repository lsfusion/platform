package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

import java.awt.*;

public class ColorPropertyRenderer extends LabelPropertyRenderer {
    Color value;
    ClientPropertyDraw property;
    public ColorPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        this.property = property;
    }

    @Override
    public void setValue(Object value) {
        this.value = (Color) value;
        getComponent().setBackground(this.value);
    }

    @Override
    public void drawBackground(boolean isInFocusedRow, boolean hasFocus, Color conditionalBackground) {
        if (value != null) {
            getComponent().setBackground(value);
        } else {
            super.drawBackground(isInFocusedRow, hasFocus, conditionalBackground);
        }
    }

    @Override
    protected void paintAsSelected() {
        getComponent().setBackground(value != null ? new Color(value.getRGB() & SwingDefaults.getTableSelectionBackground().getRGB()) : SwingDefaults.getTableSelectionBackground());
    }
}
