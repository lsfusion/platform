package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class ImagePropertyRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

    public ImagePropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(new ImageIcon((byte[]) value));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void rateSelected() {
        super.paintSelected();
    }
}
