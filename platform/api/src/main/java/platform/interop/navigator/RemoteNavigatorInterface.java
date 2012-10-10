package platform.interop.navigator;

import platform.interop.RemoteContextInterface;
import platform.interop.event.IDaemonTask;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ServerResponse;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

public interface RemoteNavigatorInterface extends PendingRemote, RemoteContextInterface {

    byte[] getNavigatorTree() throws RemoteException;

    // окна лог, релевантные классы, статус и т.п.
    byte[] getCommonWindows() throws RemoteException;

    ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException;

    ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException;

    ServerResponse throwInNavigatorAction(Exception clientException) throws RemoteException;

    RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) throws RemoteException;

    void clientExceptionLog(String info, String client, String message, String type, String erTrace) throws RemoteException;

    void close() throws RemoteException;

    // ???
    boolean showDefaultForms() throws RemoteException;

    ArrayList<String> getDefaultForms() throws RemoteException;

    // пингование сервера
    ClientCallBackInterface getClientCallBack() throws RemoteException;

    void setUpdateTime(int updateTime) throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException;

    // аутентификация
    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    void relogin(String login) throws RemoteException;

    String getCurrentUserLogin() throws RemoteException;

    void changePassword(String login, String newPassword) throws RemoteException;

    // релевантные классы
    byte[] getElementsByteArray(String groupSID) throws RemoteException;

    final static String NAVIGATORGROUP_RELEVANTFORM = "_NAV_RELEVANTFORM_";
    final static String NAVIGATORGROUP_RELEVANTCLASS = "_NAV_RELEVANTCLASS_";

    // для simple-client
    String getForms(String formSet) throws RemoteException;

    // для конфигуратора методы

    RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException;

    void saveForm(String formSID, byte[] formState) throws RemoteException;

    void saveVisualSetup(byte[] data) throws RemoteException;

    byte[] getRichDesignByteArray(String formSID) throws RemoteException;

    byte[] getFormEntityByteArray(String formSID) throws RemoteException;

    String getCurrentFormSID() throws RemoteException;
    
    Boolean getConfiguratorSecurityPolicy() throws RemoteException;
}
