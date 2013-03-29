package platform.client.form.renderer;

import platform.client.form.PropertyRenderer;

import javax.swing.*;
import java.awt.*;

public class BitPropertyRenderer extends JCheckBox implements PropertyRenderer {

    public BitPropertyRenderer() {
        super();

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

        if (!hasFocus && value == null) {
            this.setBackground(Color.lightGray);
        }
    }

    @Override
    public void paintAsSelected() {
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}
