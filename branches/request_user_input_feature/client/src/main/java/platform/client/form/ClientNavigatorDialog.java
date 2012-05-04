package platform.client.form;

import platform.interop.form.RemoteDialogInterface;

import java.awt.*;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(Component owner, RemoteDialogInterface dialog, boolean isDialog) {
        super(owner, dialog, null, isDialog);

        setUndecorated(false);
    }
}
