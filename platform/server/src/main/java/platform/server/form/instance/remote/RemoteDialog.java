package platform.server.form.instance.remote;

import platform.interop.form.RemoteDialogInterface;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;

import java.rmi.RemoteException;

public class RemoteDialog<T extends BusinessLogics<T>> extends RemoteForm<T, DialogInstance<T>> implements RemoteDialogInterface {

    public RemoteDialog(DialogInstance<T> form, FormView richDesign, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        super(form, richDesign, port, remoteFormListener);
    }

    public Object getDialogValue() {
        return form.getDialogValue();
    }

    public Object geCellDisplayValue() {
        return form.getCellDisplayValue();
    }

    public Integer getInitFilterPropertyDraw() throws RemoteException {
        if (form.initFilterPropertyDraw != null)
            return form.initFilterPropertyDraw.getID();
        else
            return null;
    }

    public Boolean isReadOnly() throws RemoteException {
        return form.readOnly;
    }
}
