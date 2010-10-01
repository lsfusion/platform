package platform.client.descriptor.view;

import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;

public class LogicsDescriptorView extends JDialog {

    public LogicsDescriptorView(Window owner, RemoteNavigatorInterface navigator) {
        super(owner, Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());
        add(new NavigatorDescriptorView(navigator));
    }
}
