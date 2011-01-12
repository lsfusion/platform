package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ActionPropertyRenderer extends JButton
                                        implements PropertyRendererComponent {

    private final Color defaultBackground = getBackground();
    private Border defaultBorder;
    private static String defaultCaption = "...";

    public ActionPropertyRenderer(String caption) {
        defaultBorder = getBorder();
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setBorder(value == null ? BorderFactory.createEmptyBorder() : defaultBorder);
        setText(value == null ? "" : defaultCaption);

        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else {
            if (value == null)
                setBackground(Color.white);
            else
                setBackground(defaultBackground);
        }
    }
}
