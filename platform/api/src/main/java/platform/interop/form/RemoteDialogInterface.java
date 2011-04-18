package platform.interop.form;

import java.rmi.RemoteException;

public interface RemoteDialogInterface extends RemoteFormInterface {

    Object getDialogValue() throws RemoteException;
    Object geCellDisplayValue() throws RemoteException;

    Integer getInitFilterPropertyDraw() throws RemoteException;
    Boolean isReadOnly() throws RemoteException;
}
