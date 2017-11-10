package lsfusion.client.form.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class DynamicFormatFileRenderer extends FilePropertyRenderer {

    public DynamicFormatFileRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        super.setValue(value, isSelected, hasFocus);
        
        if (value != null) {
            byte[] union = (byte[]) value;
            setIcon(SwingUtils.getSystemIcon(new String(union, 1, union[0])));
        }
    }
}