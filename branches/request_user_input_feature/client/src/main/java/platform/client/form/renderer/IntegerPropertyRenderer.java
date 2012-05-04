package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class IntegerPropertyRenderer extends LabelPropertyRenderer
                              implements PropertyRendererComponent {

    public IntegerPropertyRenderer(ClientPropertyDraw property) {
        super(property);

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
        setForeground(UIManager.getColor("TextField.foreground"));
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void rateSelected() {
        super.paintSelected();
    }


}
