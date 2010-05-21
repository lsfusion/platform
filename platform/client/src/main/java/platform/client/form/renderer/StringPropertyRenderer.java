package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.CellDesign;

import javax.swing.*;
import java.text.Format;
import java.awt.*;

public class StringPropertyRenderer extends LabelPropertyRenderer
                             implements PropertyRendererComponent {

    public StringPropertyRenderer(Format iformat, CellDesign design) {
        super(iformat, design);

//        setHorizontalAlignment(JLabel.LEFT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}
