package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.form.PropertyRendererComponent;
import platform.interop.CellDesign;

import javax.swing.*;
import java.text.Format;

public class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public DatePropertyRenderer(Format format, CellDesign design) {
        super(format, design);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(DateConverter.intToDate((java.sql.Date)value)));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}
