package retail.api.remote;

import platform.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

public interface RetailRemoteInterface extends RemoteLogicsInterface {
    
    PriceTransaction readNextPriceTransaction(String equServerID) throws RemoteException;

    List<ScalesInfo> readScalesInfo(String equServerID) throws RemoteException, SQLException;
}
