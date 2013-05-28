package equ.api;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

public interface EquipmentServerInterface extends Remote {

    List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException;

    List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException;

    List<TerminalInfo> readTerminalInfo(String equServerID) throws RemoteException, SQLException;

    List<TerminalDocumentTypeInfo> readTerminalDocumentTypeInfo() throws RemoteException, SQLException;

    String sendSalesInfo(List<SalesInfo> salesInfoList, String equServerID) throws IOException, SQLException;

    String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException;

    void succeedTransaction(Integer transactionID) throws RemoteException, SQLException;

    void errorTransactionReport(Integer transactionID, Exception exception) throws RemoteException, SQLException;

    void errorEquipmentServerReport(String equipmentServer, Throwable exception) throws RemoteException, SQLException;

    EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException;

    List<byte[][]> readLabelFormats (List<String> scalesModelsList) throws RemoteException, SQLException;

}
