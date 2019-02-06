package lsfusion.interop;

import lsfusion.base.NavigatorInfo;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PendingRemoteInterface;
import lsfusion.interop.remote.PreAuthentication;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    Integer getApiVersion() throws RemoteException;

    String getPlatformVersion() throws RemoteException;

    GUIPreferences getGUIPreferences() throws RemoteException;
    
    RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, long computerId) throws RemoteException;

    PreAuthentication preAuthenticateUser(String userName, String password, String language, String country) throws RemoteException;
    
    VMOptions getClientVMOptions() throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    //external requests
    List<Object> exec(String action, String[] returnCanonicalNames, Object[] params, String charset) throws RemoteException;
    List<Object> eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset) throws RemoteException;

    boolean isSingleInstance() throws RemoteException;

    long generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;

    void ping() throws RemoteException;

    void sendPingInfo(Long computerId, Map<Long, List<Long>> pingInfoMap) throws RemoteException;

    Map<String, String> readMemoryLimits() throws RemoteException;

    List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;
}
