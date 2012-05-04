package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.logics.ClientPropertyDraw;

public class CustomFileRenderer extends FilePropertyRenderer {

    public CustomFileRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            byte[] union = (byte[]) value;
            setIcon(SwingUtils.getSystemIcon(new String(union, 1, union[0])));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}