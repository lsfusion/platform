package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends JButton
        implements PropertyRendererComponent {

    private static final String defaultCaption = "...";

    private final Color defaultBackground = getBackground();
    private Border defaultBorder = getBorder();
    private Icon defaultIcon;

    public ActionPropertyRenderer() {
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (defaultIcon == null && getIcon() != null) defaultIcon = getIcon(); // временно так

        setBorder(value == null ? BorderFactory.createEmptyBorder() : defaultBorder);
        setText(defaultIcon != null || value == null ? "" : defaultCaption);
        setIcon(value == null ? null : defaultIcon);

        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128, 128, 255));
            else
                setBackground(new Color(192, 192, 255));

        } else {
            if (value == null)
                setBackground(Color.white);
            else
                setBackground(defaultBackground);
        }
    }
}
