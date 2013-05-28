package platform.client.form.renderer;

import platform.client.form.PropertyRenderer;

import javax.swing.*;
import java.awt.*;

public class LogicalPropertyRenderer extends JCheckBox implements PropertyRenderer {

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
                setBorder(SELECTED_ROW_BORDER);
                setBackground(SELECTED_ROW_BACKGROUND);
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(Color.WHITE);
        }
    }

    @Override
    public void paintAsSelected() {
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}
