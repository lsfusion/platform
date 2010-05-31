package platform.client.form;

import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.RemoteDialogInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(ClientForm owner, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(owner, dialog);

        setUndecorated(false);

        // создаем слева навигаторы
        JPanel navigatorPanel = new JPanel();
        navigatorPanel.setLayout(new BoxLayout(navigatorPanel, BoxLayout.Y_AXIS));

        navigatorPanel.add(navigator);
        navigatorPanel.add(navigator.relevantFormNavigator);
        navigatorPanel.add(navigator.relevantClassNavigator);

        add(navigatorPanel, BorderLayout.LINE_START);        
    }

    @Override
    protected boolean isDialogMode() {
        return false;
    }
}
