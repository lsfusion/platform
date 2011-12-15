package platform.client.remote.proxy;

import platform.interop.RemoteLogicsInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;

import java.rmi.RemoteException;
import java.util.TimeZone;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface>
        extends RemoteObjectProxy<T>
        implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    @NonPendingRemoteMethod
    public RemoteNavigatorInterface createNavigator(String login, String password, int computer, boolean forceCreateNew) throws RemoteException {
        RemoteNavigatorInterface remote = target.createNavigator(login, password, computer, forceCreateNew);
        if (remote == null) {
            return null;
        }
        return new RemoteNavigatorProxy(remote);
    }

    public String getName() throws RemoteException {
        logRemoteMethodStartCall("getName");
        String result = target.getName();
        logRemoteMethodEndCall("getName", result);
        return result;
    }

    public String getDisplayName() throws RemoteException {
        logRemoteMethodStartCall("getDisplayName");
        String result = target.getDisplayName();
        logRemoteMethodEndCall("getDisplayName", result);
        return result;
    }

    public byte[] getMainIcon() throws RemoteException {
        logRemoteMethodStartCall("getMainIcon");
        byte[] result = target.getMainIcon();
        logRemoteMethodEndCall("getMainIcon", result);
        return result;
    }

    public byte[] getLogo() throws RemoteException {
        logRemoteMethodStartCall("getLogo");
        byte[] result = target.getLogo();
        logRemoteMethodEndCall("getLogo", result);
        return result;
    }

    public Integer getComputer(String hostname) throws RemoteException {
        logRemoteMethodStartCall("getComputer");
        Integer result = target.getComputer(hostname);
        logRemoteMethodEndCall("getComputer", result);
        return result;
    }

    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreen");
        ExternalScreen result = target.getExternalScreen(screenID);
        logRemoteMethodEndCall("getExternalScreen", result);
        return result;        
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreenParameters");
        ExternalScreenParameters result = target.getExternalScreenParameters(screenID, computerId);
        logRemoteMethodEndCall("getExternalScreenParameters", result);
        return result;
    }

    public void endSession(String clientInfo) throws RemoteException {
        target.endSession(clientInfo);
    }

    public boolean checkUser(String login, String password) throws RemoteException {
        return target.checkUser(login, password);
    }

    @Override
    public boolean getUseUniPass() throws RemoteException {
        return false;
    }

    public TimeZone getTimeZone() throws RemoteException {
        return target.getTimeZone();
    }

    public byte[] getPropertyObjectsByteArray(byte[] classes, boolean isCompulsory, boolean isAny) throws RemoteException {
        return target.getPropertyObjectsByteArray(classes, isCompulsory, isAny);
    }

    public byte[] getBaseClassByteArray() throws RemoteException {
        return target.getBaseClassByteArray();
    }

    public int generateNewID()  throws RemoteException {
        return target.generateNewID();
    }

    @NonFlushRemoteMethod
    public void ping() throws RemoteException {
        target.ping();
    }

    @NonFlushRemoteMethod
    public byte[] findClass(String name) throws RemoteException {
        logRemoteMethodStartCall("findClass");
        byte[] result = target.findClass(name);
        logRemoteMethodEndCall("findClass", result);
        return result;
    }

    public UserInfo getUserInfo(String username) throws RemoteException {
        logRemoteMethodStartCall("getUserInfo");
        UserInfo result = target.getUserInfo(username);
        logRemoteMethodEndCall("getUserInfo", result);
        return result;
    }

    public void remindPassword(String email) throws RemoteException {
        logRemoteMethodStartCall("remindPassword");
        target.remindPassword(email);
        logRemoteMethodEndVoidCall("remindPassword");
    }

    public String getRemoteActionMessage() throws RemoteException {
        return target.getRemoteActionMessage();
    }

    public byte[] readFile(String sid, String... params) throws RemoteException {
        logRemoteMethodStartCall("readFile");
        byte[] result = target.readFile(sid, params);
        logRemoteMethodEndVoidCall("readFile");
        return result;
    }

    public boolean checkDefaultViewPermission(String propertySid) throws RemoteException {
        logRemoteMethodStartCall("checkDefaultViewPermission");
        boolean result = target.checkDefaultViewPermission(propertySid);
        logRemoteMethodEndVoidCall("checkDefaultViewPermission");
        return result;
    }

    public boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException {
        logRemoteMethodStartCall("checkPropertyViewPermission");
        boolean result = target.checkPropertyViewPermission(userName, propertySID);
        logRemoteMethodEndVoidCall("checkPropertyViewPermission");
        return result;
    }
}
