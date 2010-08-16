package platform.server.form.instance.remote;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.interop.form.RemoteDialogInterface;
import platform.server.form.instance.listener.CurrentClassListener;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.form.instance.DialogInstance;

import java.rmi.RemoteException;

public class RemoteDialog<T extends BusinessLogics<T>> extends RemoteForm<T, DialogInstance<T>> implements RemoteDialogInterface {

    public RemoteDialog(DialogInstance<T> form, FormView richDesign, JasperDesign reportDesign, int port, CurrentClassListener currentClassListener) throws RemoteException {
        super(form, richDesign, reportDesign, port, currentClassListener);
    }

    public Object getDialogValue() {
        return form.getDialogValue();
    }
}
