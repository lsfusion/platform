package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import java.awt.*;

public class LogicalPropertyRenderer extends JCheckBox
                          implements PropertyRendererComponent {

    public LogicalPropertyRenderer() {
        super();

        setHorizontalAlignment(JCheckBox.CENTER);

        setOpaque(true);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setSelected(value!=null);

        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(Color.white);
    }
}
