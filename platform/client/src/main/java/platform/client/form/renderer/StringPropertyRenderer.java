package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.StringPropertyEditor;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.text.Format;

public class StringPropertyRenderer extends LabelPropertyRenderer
        implements PropertyRendererComponent {

    public static final String EMPTY_STRING = "Неопределено";

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
            setText(value.toString());
        } else {
            setForeground(inactiveForeground);
            setText(EMPTY_STRING);
        }
        setSelected(isSelected, hasFocus);
    }
}
