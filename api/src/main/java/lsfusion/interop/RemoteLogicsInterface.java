package lsfusion.interop;

import lsfusion.base.ExecResult;
import lsfusion.base.NavigatorInfo;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PendingRemoteInterface;
import lsfusion.interop.remote.PreAuthentication;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    GUIPreferences getGUIPreferences() throws RemoteException;
    
    RemoteNavigatorInterface createNavigator(NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException;

    PreAuthentication preAuthenticateUser(String userName, String password, String language, String country) throws RemoteException;
    
    VMOptions getClientVMOptions() throws RemoteException;

    //external requests
    ExecResult exec(String action, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;
    ExecResult eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;

    boolean isSingleInstance() throws RemoteException;

    long generateID() throws RemoteException;

    void ping() throws RemoteException;

    void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) throws RemoteException;

    List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;
}
