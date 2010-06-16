package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.interop.form.RemoteDialogInterface;
import platform.server.logics.BusinessLogics;
import platform.server.view.form.RemoteDialog;

import java.rmi.RemoteException;

public class RemoteDialogView<T extends BusinessLogics<T>> extends RemoteFormView<T,RemoteDialog<T>> implements RemoteDialogInterface {

    public RemoteDialogView(RemoteDialog<T> form, FormView richDesign, JasperDesign reportDesign, int port) throws RemoteException {
        super(form, richDesign, reportDesign, port);
    }

    public Object getDialogValue() {
        return form.getDialogValue();
    }
}
