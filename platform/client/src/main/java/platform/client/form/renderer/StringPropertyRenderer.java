package platform.client.form.renderer;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.text.Format;

public class StringPropertyRenderer extends LabelPropertyRenderer
        implements PropertyRendererComponent {

    public static final String EMPTY_STRING = ClientResourceBundle.getString("form.renderer.not.defined");

    private static final Color normalForeground = UIManager.getColor("TextField.foreground");
    private static final Color inactiveForeground = UIManager.getColor("TextField.inactiveForeground");

    public StringPropertyRenderer(Format iformat, ComponentDesign design) {
        super(iformat, design);

//        setHorizontalAlignment(JLabel.LEFT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setForeground(normalForeground);
            setText(BaseUtils.rtrim(value.toString()));
        } else {
            setForeground(inactiveForeground);
            setText(EMPTY_STRING);
        }
        setSelected(isSelected, hasFocus);
    }
}
