package platform.client.form;

import platform.base.DateConverter;

import javax.swing.*;
import java.text.Format;

public class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public DatePropertyRenderer(Format format) {
        super(format);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(DateConverter.intToDate((Integer)value)));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}
