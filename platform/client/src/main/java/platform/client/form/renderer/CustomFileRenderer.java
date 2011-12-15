package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.text.Format;

public class CustomFileRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

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

    @Override
    public void rateSelected() {
        super.paintSelected();
    }
}