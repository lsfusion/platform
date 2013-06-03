package lsfusion.client.descriptor.view;

import lsfusion.client.navigator.ClientNavigator;

import javax.swing.*;
import java.awt.*;

public class LogicsDescriptorView extends JDialog {

    public LogicsDescriptorView(Window owner, ClientNavigator navigator) {
        super(owner, Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());
        add(new NavigatorDescriptorView(navigator));
    }
}
