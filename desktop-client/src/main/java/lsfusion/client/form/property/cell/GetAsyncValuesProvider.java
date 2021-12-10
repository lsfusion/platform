package lsfusion.client.form.property.cell;

import java.rmi.RemoteException;

public interface GetAsyncValuesProvider {
    ClientAsync[] getAsyncValues(int propertyID, byte[] columnKey, String actionSID, String value, int asyncIndex) throws RemoteException;
}
