package lsfusion.base.lambda;

import java.rmi.RemoteException;

public interface GetAsyncValuesProvider {
    String[] getAsyncValues(int propertyID, byte[] columnKey, String actionSID, String value, int asyncIndex) throws RemoteException;
}
