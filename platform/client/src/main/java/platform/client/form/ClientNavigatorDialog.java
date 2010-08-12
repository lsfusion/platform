package platform.client.form;

import platform.interop.form.RemoteDialogInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(Component owner, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(owner, dialog);

        setUndecorated(false);
    }

    @Override
    protected boolean isReadOnlyMode() {
        return false;
    }
}
