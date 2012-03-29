package retail.api.remote;

import platform.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface RetailRemoteInterface extends RemoteLogicsInterface {
    
    PriceTransaction readNextPriceTransaction(String equServerID) throws RemoteException;
}
