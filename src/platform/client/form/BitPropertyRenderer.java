package platform.client.form;

import javax.swing.*;
import java.awt.*;

public class BitPropertyRenderer extends JCheckBox
                          implements PropertyRendererComponent {

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
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(Color.white);

        if (!hasFocus && value == null) {
            this.setBackground(Color.lightGray);
        }
    }
}
