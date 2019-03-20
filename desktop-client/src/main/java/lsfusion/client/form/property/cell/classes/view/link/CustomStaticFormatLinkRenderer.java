package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class CustomStaticFormatLinkRenderer extends LinkPropertyRenderer {

    private String drawExtension;

    public CustomStaticFormatLinkRenderer(ClientPropertyDraw property, String drawExtension) {
        super(property);

        this.drawExtension = drawExtension;
    }

    public void setValue(Object value) {
        if (drawExtension != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon(drawExtension));
        } else {
            getComponent().setIcon(null);
        }
        super.setValue(value);
    }
}