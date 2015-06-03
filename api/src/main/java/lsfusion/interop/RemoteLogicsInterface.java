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
    
    GUIPreferences getGUIPreferences() throws RemoteException;
    
    String getUserTimeZone() throws RemoteException;

    RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException;

    Integer getComputer(String hostname) throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException;

    List<String> authenticateUser(String userName, String password) throws RemoteException;

    VMOptions getClientVMOptions() throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    byte[] readFile(String sid, String... params) throws RemoteException;

    void runAction(String sid, String... params) throws RemoteException;

    boolean checkDefaultViewPermission(String propertySid) throws RemoteException;

    boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException;

    boolean checkPropertyChangePermission(String userName, String propertySID) throws RemoteException;

    boolean checkFormExportPermission(String canonicalName) throws RemoteException;

    int generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;

    void ping() throws RemoteException;

    int generateNewID() throws RemoteException;

    void sendPingInfo(Integer computerId, Map<Long, List<Long>> pingInfoMap) throws RemoteException;
}
