package platform.client.form.renderer;

import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class ImagePropertyRenderer extends FilePropertyRenderer {

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(new ImageIcon((byte[]) value));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
