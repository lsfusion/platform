package platform.client.form;

import platform.interop.form.RemoteDialogInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(Component owner, RemoteNavigatorInterface remoteNavigator, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(owner, remoteNavigator, dialog);

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
    protected boolean isReadOnlyMode() {
        return false;
    }
}
