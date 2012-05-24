package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.logics.ClientPropertyDraw;

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
