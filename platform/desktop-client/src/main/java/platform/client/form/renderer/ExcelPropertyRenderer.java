package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.logics.ClientPropertyDraw;

public class ExcelPropertyRenderer extends FilePropertyRenderer {

    public ExcelPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("xls"));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
