package platform.client.form;

import platform.interop.form.RemoteDialogInterface;

import java.awt.*;

public class ClientNavigatorDialog extends ClientDialog {

    public ClientNavigatorDialog(Component owner, RemoteDialogInterface dialog) {
        super(owner, dialog, null);
    }

    @Override
    protected void setupUndecorated() {
        //пропускаем, потому что здесь должен быть нормальный диалог
    }
}
