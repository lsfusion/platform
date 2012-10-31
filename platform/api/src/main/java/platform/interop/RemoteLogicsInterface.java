package platform.interop;

import platform.base.serialization.RemoteDescriptorInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;
import platform.interop.remote.PendingRemote;
import platform.interop.remote.PingRemote;

import java.rmi.RemoteException;
import java.util.TimeZone;

public interface RemoteLogicsInterface extends PendingRemote, PingRemote, RemoteDescriptorInterface, RemoteContextInterface {

    String getName() throws RemoteException;

    String getDisplayName() throws RemoteException;

    byte[] getMainIcon() throws RemoteException;

    byte[] getLogo() throws RemoteException;

    RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, boolean forceCreateNew) throws RemoteException;

    Integer getComputer(String hostname) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException;
    
    void endSession(String clientInfo) throws RemoteException;

    boolean checkUser(String login, String password) throws RemoteException;

    void ping() throws RemoteException;

    TimeZone getTimeZone() throws RemoteException;

    UserInfo getUserInfo(String username) throws RemoteException;

    void remindPassword(String email, String localeLanguage) throws RemoteException;

    byte[] readFile(String sid, String... params) throws RemoteException;

    boolean checkDefaultViewPermission(String propertySid) throws RemoteException;

    boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException;
    
    int generateID() throws RemoteException;

    String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException;
}
