package platform.client.form.renderer;

import platform.client.form.PropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends JButton implements PropertyRenderer {
    private static final String defaultCaption = "...";

    private final Color defaultBackground = getBackground();
    private Border defaultBorder = getBorder();
    private Icon defaultIcon;

    public ActionPropertyRenderer(ClientPropertyDraw property) {
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

        if (isSelected) {
            if (hasFocus) {
                setBorder(FOCUSED_CELL_BORDER);
                setBackground(FOCUSED_CELL_BACKGROUND);
            }
            else {
                setBorder(SELECTED_ROW_BORDER);
                setBackground(SELECTED_ROW_BACKGROUND);
            }
        } else {
            setBorder(value == null ? BorderFactory.createEmptyBorder() : defaultBorder);
            setBackground(value == null ? Color.white : defaultBackground);
        }
    }

    @Override
    public void paintAsSelected() {
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}
