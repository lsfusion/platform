package platform.client.form;

import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import java.text.Format;

public class StringPropertyRenderer extends LabelPropertyRenderer
                             implements PropertyRendererComponent {

    public StringPropertyRenderer(Format iformat) {
        super(iformat);

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
