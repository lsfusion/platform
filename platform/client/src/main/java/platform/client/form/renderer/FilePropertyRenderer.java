package platform.client.form.renderer;

import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.text.Format;

public class FilePropertyRenderer extends LabelPropertyRenderer {

    public FilePropertyRenderer(ClientPropertyDraw property) {
        super(property);

        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
    }

    public JComponent getComponent() {
        return this;
    }

    @Override
    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus)
                setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(128, 128, 255)));
            else
                setBorder(null);

        } else
            setBorder(null);
    }
}
