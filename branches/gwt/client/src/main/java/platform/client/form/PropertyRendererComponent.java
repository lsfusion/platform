package platform.client.form;

import javax.swing.*;

public interface PropertyRendererComponent {

    JComponent getComponent();

    void setValue(Object value, boolean isSelected, boolean hasFocus);

}


