package lsfusion.interop;

import lsfusion.base.FileData;
import lsfusion.base.NavigatorInfo;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    Integer getApiVersion() throws RemoteException;

    String getPlatformVersion() throws RemoteException;

    GUIPreferences getGUIPreferences() throws RemoteException;
    
    RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException;
    
    Set<String> syncUsers(Set<String> userNames) throws RemoteException;

    Long getComputer(String hostname) throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(long compId) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, long computerId) throws RemoteException;

    List<String> authenticateUser(String userName, String password) throws RemoteException;

    VMOptions getClientVMOptions() throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    //external requests
    List<Object> exec(String action, String[] returnCanonicalNames, Object[] params, Charset charset) throws RemoteException;
    List<Object> eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset) throws RemoteException;
    List<Object> read(String property, Object[] params, Charset charset) throws RemoteException;

    boolean isSingleInstance() throws RemoteException;

    long generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;

    void ping() throws RemoteException;

    void sendPingInfo(Long computerId, Map<Long, List<Long>> pingInfoMap) throws RemoteException;

    Map<String, String> readMemoryLimits() throws RemoteException;

    List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;
}
