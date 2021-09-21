package lsfusion.base.lambda;

import lsfusion.interop.form.property.cell.Async;

import java.rmi.RemoteException;

public interface GetAsyncValuesProvider {
    Async[] getAsyncValues(int propertyID, byte[] columnKey, String actionSID, String value, int asyncIndex) throws RemoteException;
}
