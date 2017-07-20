package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientColorClass;

import javax.swing.*;
import java.awt.*;

public class ColorPropertyRenderer extends LabelPropertyRenderer {
    Color value;
    ClientPropertyDraw property;
    public ColorPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        this.property = property;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        this.value = value == null ? ClientColorClass.getDefaultValue() : (Color) value;
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void drawBackground(boolean isSelected, boolean hasFocus) {
        if (isSelected && property != null) {
            if (hasFocus) {
                setBackground(new Color(value.getRGB() & property.colorPreferences.getFocusedCellBackground().getRGB()));
            } else {
                setBackground(new Color(value.getRGB() & property.colorPreferences.getSelectedRowBackground().getRGB()));
            }
        } else {
            setBackground(value);
        }
    }

    @Override
    public void paintAsSelected() {
        if (property != null) setBackground(new Color(value.getRGB() & property.colorPreferences.getSelectedCellBackground().getRGB()));
    }
}
