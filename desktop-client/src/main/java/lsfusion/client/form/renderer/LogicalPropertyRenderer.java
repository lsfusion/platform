package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

public class LogicalPropertyRenderer extends JCheckBox implements PropertyRenderer {
    protected ClientPropertyDraw property;
    
    public LogicalPropertyRenderer(ClientPropertyDraw property) {
        super();
        this.property = property;
        setHorizontalAlignment(JCheckBox.CENTER);
        setBorderPainted(true);
        setOpaque(true);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setSelected(value != null);

        if (isSelected && property != null) {
            if (hasFocus) {
                setBorder(property.colorPreferences.getFocusedCellBorder());
                setBackground(property.colorPreferences.getFocusedCellBackground());
            }
            else {
                setBorder(property.colorPreferences.getSelectedRowBorder());
                setBackground(property.colorPreferences.getSelectedRowBackground());
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(Color.WHITE);
        }
    }

    @Override
    public void paintAsSelected() {
        setBackground(property.colorPreferences.getSelectedCellBackground());
    }
}
