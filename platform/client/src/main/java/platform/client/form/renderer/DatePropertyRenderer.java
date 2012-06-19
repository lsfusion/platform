package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class DatePropertyRenderer extends LabelPropertyRenderer {

    public DatePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setText(value == null ? "" : format.format(DateConverter.sqlToDate((java.sql.Date) value)));
        setForeground(UIManager.getColor("TextField.foreground"));
        setSelected(isSelected, hasFocus);
    }
}
