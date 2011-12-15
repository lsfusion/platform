package platform.client.form.renderer;

import platform.base.DateConverter;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.sql.Timestamp;
import java.text.Format;

public class DateTimePropertyRenderer extends LabelPropertyRenderer
        implements PropertyRendererComponent {

    public DateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(DateConverter.stampToDate((Timestamp) value)));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void rateSelected() {
        super.paintSelected();
    }

}
