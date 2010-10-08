package platform.interop.serialization;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteDescriptorInterface extends Remote {

    // получает имлементации подходящие хотя бы одному из классов
    byte[] getPropertyObjectsByteArray(byte[] classes) throws RemoteException;
}
