package platform.client.form.renderer;

import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class TimePropertyRenderer extends LabelPropertyRenderer {
    public TimePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.RIGHT);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setText(value == null ? "" : format.format(value));
        setForeground(UIManager.getColor("TextField.foreground"));
        setSelected(isSelected, hasFocus);
    }
}
