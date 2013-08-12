package lsfusion.interop.navigator;

import lsfusion.base.DefaultForms;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteNavigatorInterface extends PendingRemoteInterface {

    public static final String NAVIGATORGROUP_RELEVANTFORM = "_NAV_RELEVANTFORM_";
    public static final String NAVIGATORGROUP_RELEVANTCLASS = "_NAV_RELEVANTCLASS_";

    byte[] getNavigatorTree() throws RemoteException;

    // окна лог, релевантные классы, статус и т.п.
    byte[] getCommonWindows() throws RemoteException;

    ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException;

    ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException;

    ServerResponse throwInNavigatorAction(Exception clientException) throws RemoteException;

    RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) throws RemoteException;

    void logClientException(String info, String client, String message, String type, String erTrace) throws RemoteException;

    void close() throws RemoteException;

    // ???
    DefaultForms showDefaultForms() throws RemoteException;

    List<String> getDefaultForms() throws RemoteException;

    // пингование сервера
    ClientCallBackInterface getClientCallBack() throws RemoteException;

    void setUpdateTime(int updateTime) throws RemoteException;

    // аутентификация
    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    // релевантные элементы
    byte[] getElementsByteArray(String groupSID) throws RemoteException;

    // для конфигуратора методы

    RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException;

    byte[] getRichDesignByteArray(String formSID) throws RemoteException;

    byte[] getFormEntityByteArray(String formSID) throws RemoteException;

    String getCurrentFormSID() throws RemoteException;
    
    boolean isConfigurationAccessAllowed() throws RemoteException;
}
