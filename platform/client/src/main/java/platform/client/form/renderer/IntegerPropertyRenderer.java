package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.CellDesign;

import javax.swing.*;
import java.text.Format;
import java.awt.*;

public class IntegerPropertyRenderer extends LabelPropertyRenderer
                              implements PropertyRendererComponent {

    public IntegerPropertyRenderer(Format format, CellDesign design) {
        super(format, design);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(value));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }


}
