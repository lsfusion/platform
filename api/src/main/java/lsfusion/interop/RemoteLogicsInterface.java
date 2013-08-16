package lsfusion.interop;

import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PendingRemoteInterface;
import lsfusion.interop.remote.UserInfo;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TimeZone;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    String getName() throws RemoteException;

    String getDisplayName() throws RemoteException;

    byte[] getMainIcon() throws RemoteException;

    byte[] getLogo() throws RemoteException;

    RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean forceCreateNew) throws RemoteException;

    Integer getComputer(String hostname) throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException;
    
    void endSession(String clientInfo) throws RemoteException;

    TimeZone getTimeZone() throws RemoteException;

    UserInfo getUserInfo(String username) throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    byte[] readFile(String sid, String... params) throws RemoteException;

    void runAction(String sid, String... params) throws RemoteException;

    boolean checkDefaultViewPermission(String propertySid) throws RemoteException;

    boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException;

    boolean checkPropertyChangePermission(String userName, String propertySID) throws RemoteException;

    boolean checkFormExportPermission(String formSid) throws RemoteException;
    
    int generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;

    void ping() throws RemoteException;

    // получает имлементации подходящие хотя бы одному из классов или по всем
    byte[] getPropertyObjectsByteArray(byte[] classes, boolean isCompulsory, boolean isAny) throws RemoteException;

    byte[] getBaseClassByteArray() throws RemoteException;

    int generateNewID() throws RemoteException;
}
