package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class CustomStaticFormatFileRenderer extends FilePropertyRenderer {
    
    private String drawExtension;

    public CustomStaticFormatFileRenderer(ClientPropertyDraw property, String drawExtension) {
        super(property);

        this.drawExtension = drawExtension;
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (drawExtension != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon(drawExtension));
        } else {
            getComponent().setIcon(null);
        }
    }
}
