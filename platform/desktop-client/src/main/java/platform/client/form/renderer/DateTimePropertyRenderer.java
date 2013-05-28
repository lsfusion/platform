package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.sql.Timestamp;

public class DateTimePropertyRenderer extends LabelPropertyRenderer {

    public DateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setText(value == null ? "" : format.format(DateConverter.stampToDate((Timestamp) value)));
        setForeground(UIManager.getColor("TextField.foreground"));
        setSelected(isSelected, hasFocus);
    }
}
