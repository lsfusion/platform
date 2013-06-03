package lsfusion.client.form.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class CustomStaticFormatFileRenderer extends FilePropertyRenderer {
    
    private String drawExtension;

    public CustomStaticFormatFileRenderer(ClientPropertyDraw property, String drawExtension) {
        super(property);

        this.drawExtension = drawExtension;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (drawExtension != null) {
            setIcon(SwingUtils.getSystemIcon(drawExtension));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
