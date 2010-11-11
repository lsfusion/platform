package platform.client.descriptor.view;

import platform.client.navigator.ClientNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LogicsDescriptorView extends JDialog {

    public LogicsDescriptorView(Window owner, ClientNavigator navigator) {
        super(null, Dialog.ModalityType.MODELESS); // обозначаем parent'а и модальность

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setAlwaysOnTop(false);

        setLayout(new BorderLayout());
        add(new NavigatorDescriptorView(navigator));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
