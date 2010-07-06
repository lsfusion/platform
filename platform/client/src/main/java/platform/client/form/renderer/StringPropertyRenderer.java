package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class StringPropertyRenderer extends LabelPropertyRenderer
                             implements PropertyRendererComponent {

    public StringPropertyRenderer(Format iformat, ComponentDesign design) {
        super(iformat, design);

//        setHorizontalAlignment(JLabel.LEFT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}
