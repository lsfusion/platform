package lsfusion.client.form.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

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
        super.setValue(value, isSelected, hasFocus);

        if (value != null || !property.isEditableNotNull()) {
            setText(value == null ? "" : format.format(DateConverter.sqlToDate((java.sql.Date) value)));
        }
    }
}
