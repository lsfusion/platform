package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import java.awt.*;

public class LogicalPropertyRenderer extends JCheckBox
                          implements PropertyRendererComponent {

    public LogicalPropertyRenderer() {
        super();

        setHorizontalAlignment(JCheckBox.CENTER);
        setBorderPainted(true);
        setOpaque(true);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setSelected(value != null);

        if (isSelected) {
            if (hasFocus) {
                setBorder(FOCUSED_CELL_BORDER);
                setBackground(FOCUSED_CELL_BACKGROUND);
            }
            else {
                setBorder(SELECTED_CELL_BORDER);
                setBackground(SELECTED_CELL_BACKGROUND);
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(Color.WHITE);
        }
    }
}
