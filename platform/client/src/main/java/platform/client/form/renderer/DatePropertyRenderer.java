package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.Main;
import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public DatePropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            Calendar calendar = Calendar.getInstance(Main.timeZone);
            calendar.setTime(DateConverter.sqlToDate((java.sql.Date) value));
            setText(format.format(calendar.getTime()));
        } else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}
