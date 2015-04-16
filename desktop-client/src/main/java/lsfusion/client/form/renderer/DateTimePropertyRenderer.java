package lsfusion.client.form.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

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
        super.setValue(value, isSelected, hasFocus);

        if (value != null || !property.isEditableNotNull()) {
            setText(value == null ? "" : format.format(DateConverter.stampToDate((Timestamp) value)));
        }
    }
}
