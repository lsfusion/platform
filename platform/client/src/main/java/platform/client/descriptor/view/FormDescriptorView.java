package platform.client.descriptor.view;

import platform.client.descriptor.FormDescriptor;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;

public class FormDescriptorView extends JPanel {

    FormDescriptor model;

    public FormDescriptorView() {

        setPreferredSize(new Dimension(1000, getPreferredSize().height));
    }

    public void setModel(FormDescriptor model) {
        this.model = model;
        update();
    }

    private void update() {
        
    }
}
