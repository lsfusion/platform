package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.interop.form.ColorPreferences;

import javax.swing.*;
import java.awt.*;

public class BitPropertyRenderer extends JCheckBox implements PropertyRenderer {
    private ColorPreferences colorPreferences;
    
    public BitPropertyRenderer(ColorPreferences colorPreferences) {
        super();
        this.colorPreferences = colorPreferences;

        setHorizontalAlignment(JCheckBox.CENTER);

        setOpaque(true);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setSelected((Boolean)value);
        else
            setSelected(false);

        if (isSelected) {
            if (hasFocus) {
                setBorder(colorPreferences.getFocusedCellBorder());
                setBackground(colorPreferences.getFocusedCellBackground());
            }
            else {
                setBorder(colorPreferences.getSelectedRowBorder());
                setBackground(colorPreferences.getSelectedRowBackground());
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(Color.WHITE);
        }

        if (!hasFocus && value == null) {
            this.setBackground(Color.lightGray);
        }
    }

    @Override
    public void paintAsSelected() {
        setBackground(colorPreferences.getSelectedCellBackground());
    }
}
