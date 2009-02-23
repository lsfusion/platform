package platform.client.form;

import java.awt.*;

public interface PropertyEditorComponent {

    Component getComponent();

    Object getCellEditorValue();
    boolean valueChanged();

}


