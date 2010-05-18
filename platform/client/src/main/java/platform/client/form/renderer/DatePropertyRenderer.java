package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import java.text.Format;
import java.awt.*;

public class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public DatePropertyRenderer(Format format, Font font) {
        super(format, font);

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
