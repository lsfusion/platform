package lsfusion.interop;

import lsfusion.base.NavigatorInfo;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    Integer getApiVersion() throws RemoteException;
    
    GUIPreferences getGUIPreferences() throws RemoteException;
    
    RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException;

    Long getComputer(String hostname) throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(long compId) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, long computerId) throws RemoteException;

    List<String> authenticateUser(String userName, String password) throws RemoteException;

    VMOptions getClientVMOptions() throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    byte[] readFile(String canonicalName, String... params) throws RemoteException;

    List<Object> exec(String action, String[] returnCanonicalNames, Object[] params) throws RemoteException;

    List<Object> eval(String script, String[] returnCanonicalNames, Object[] params) throws RemoteException;

    boolean checkDefaultViewPermission(String propertySid) throws RemoteException;

    boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException;

    boolean checkPropertyChangePermission(String userName, String propertySID) throws RemoteException;

    boolean checkFormExportPermission(String canonicalName) throws RemoteException;
    
    String getFormCanonicalName(String navigatorElementCanonicalName) throws RemoteException; 

    boolean isSingleInstance() throws RemoteException;

    boolean isBusyDialog() throws RemoteException;

    long generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;

    void ping() throws RemoteException;

    int generateNewID() throws RemoteException;

    void sendPingInfo(Long computerId, Map<Long, List<Long>> pingInfoMap) throws RemoteException;

    Map<String, String> readMemoryLimits() throws RemoteException;
}
