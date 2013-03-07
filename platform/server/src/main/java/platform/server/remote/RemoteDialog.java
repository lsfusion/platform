package platform.server.remote;

import platform.interop.form.RemoteDialogInterface;
import platform.interop.remote.SelectedObject;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.logics.BusinessLogics;

import java.rmi.RemoteException;

public class RemoteDialog<T extends BusinessLogics<T>> extends RemoteForm<T, DialogInstance<T>> implements RemoteDialogInterface {

    public RemoteDialog(DialogInstance<T> form, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        super(form, port, remoteFormListener);
    }

    @Override
    public SelectedObject getSelectedObject() throws RemoteException {
        Object dialogValue = form.getDialogValue();
        return new SelectedObject(dialogValue, (dialogValue == null) ? null : form.getCellDisplayValue());
    }

    public Integer getInitFilterPropertyDraw() throws RemoteException {
        if (form.initFilterPropertyDraw != null)
            return form.initFilterPropertyDraw.getID();
        else
            return null;
    }

    @Override
    public Boolean isUndecorated() throws RemoteException {
        return form.undecorated;
    }
}
