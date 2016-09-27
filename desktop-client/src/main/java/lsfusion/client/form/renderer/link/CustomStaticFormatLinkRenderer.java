package lsfusion.client.form.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.form.renderer.FilePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

public class CustomStaticFormatLinkRenderer extends LinkPropertyRenderer {

    private String drawExtension;

    public CustomStaticFormatLinkRenderer(ClientPropertyDraw property, String drawExtension) {
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