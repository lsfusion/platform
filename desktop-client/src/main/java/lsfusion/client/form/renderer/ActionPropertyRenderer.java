package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends JButton implements PropertyRenderer {
    private static final String defaultCaption = "...";

    private final Color defaultBackground = getBackground();
    private Border defaultBorder = getBorder();
    private Icon defaultIcon;
    private ClientPropertyDraw property;

    public ActionPropertyRenderer(ClientPropertyDraw property) {
        this.property = property;
        property.design.designComponent(this);
        setFocusPainted(false);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (defaultIcon == null && getIcon() != null) defaultIcon = getIcon(); // временно так

        setText(defaultIcon != null || value == null ? "" : defaultCaption);
        setIcon(value == null ? null : defaultIcon);

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
            setBorder(value == null ? BorderFactory.createEmptyBorder() : defaultBorder);
            setBackground(value == null ? Color.white : defaultBackground);
        }
    }

    @Override
    public void paintAsSelected() {
        if (property != null) setBackground(property.colorPreferences.getSelectedCellBackground());
    }
}
