package platform.interop.form;

import platform.interop.remote.SelectedObject;

import java.rmi.RemoteException;

public interface RemoteDialogInterface extends RemoteFormInterface {
    SelectedObject getSelectedObject() throws RemoteException;
    Integer getInitFilterPropertyDraw() throws RemoteException;
    Boolean isUndecorated() throws RemoteException;
}
