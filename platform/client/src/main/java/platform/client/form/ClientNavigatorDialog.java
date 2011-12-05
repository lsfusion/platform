package platform.client.form;

import platform.interop.form.RemoteDialogInterface;

import java.awt.*;
import java.io.IOException;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(Component owner, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(owner, dialog);

        setUndecorated(false);
    }
}
