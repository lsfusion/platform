package lsfusion.client.form.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.sql.Timestamp;

public class DateTimePropertyRenderer extends LabelPropertyRenderer {

    public DateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        getComponent().setHorizontalAlignment(JLabel.RIGHT);
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null || property == null || !property.isEditableNotNull()) {
            getComponent().setText(value == null ? "" : format.format(DateConverter.stampToDate((Timestamp) value)));
        }
    }
}
