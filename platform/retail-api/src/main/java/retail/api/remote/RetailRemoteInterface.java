package retail.api.remote;

import platform.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

public interface RetailRemoteInterface extends RemoteLogicsInterface {

    List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException;

    List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException;

    String sendSalesInfo(List<SalesInfo> salesInfoList, String equServerID) throws IOException, SQLException;
    
    void succeedTransaction(Integer transactionID) throws RemoteException, SQLException;

    void errorTransactionReport(Integer transactionID, Exception exception) throws RemoteException, SQLException;

    void errorEquipmentServerReport(String equipmentServer, String message) throws RemoteException, SQLException;
    
    Integer readEquipmentServerDelay(String equipmentServer) throws RemoteException, SQLException;
   
}
