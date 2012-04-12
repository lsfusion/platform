package retail.api.remote;

import platform.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

public interface RetailRemoteInterface extends RemoteLogicsInterface {

    List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException;

    void succeedTransaction(Integer transactionID) throws RemoteException, SQLException;

    void errorReport(Integer transactionID, Exception exception) throws RemoteException, SQLException;
}
