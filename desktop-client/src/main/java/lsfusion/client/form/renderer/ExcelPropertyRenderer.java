package lsfusion.client.form.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class ExcelPropertyRenderer extends FilePropertyRenderer {

    public ExcelPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("xls"));
            setText(null);
        } else {
            setIcon(null);
            if (property.isEditableNotNull()) {
                setText(REQUIRED_STRING);
                setForeground(REQUIRED_FOREGROUND);
            }
        }
        setSelected(isSelected, hasFocus);
    }
}
