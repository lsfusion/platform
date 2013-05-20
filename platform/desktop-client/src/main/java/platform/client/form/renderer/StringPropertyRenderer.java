package platform.client.form.renderer;

import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

public class StringPropertyRenderer extends LabelPropertyRenderer {

    private final boolean echoSymbols;

    private static final Color normalForeground = UIManager.getColor("TextField.foreground");
    private static final Color inactiveForeground = UIManager.getColor("TextField.inactiveForeground");

    public StringPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        echoSymbols = property != null && property.echoSymbols;

//        setHorizontalAlignment(JLabel.LEFT);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setForeground(normalForeground);
            setText(echoSymbols ? "******" : value.toString());
        } else {
            setForeground(inactiveForeground);
            setText(EMPTY_STRING);
        }
        setSelected(isSelected, hasFocus);
    }
}
