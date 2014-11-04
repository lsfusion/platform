package lsfusion.client.remote.proxy;

import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface> extends RemoteObjectProxy<T> implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean forceCreateNew) throws RemoteException {
        RemoteNavigatorInterface remote = target.createNavigator(isFullClient, login, password, computer, remoteAddress, forceCreateNew);
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

    @Override
    public String getUserTimeZone() throws RemoteException {
        logRemoteMethodStartCall("getUserTimeZone");
        String result = target.getUserTimeZone();
        logRemoteMethodEndCall("getUserTimeZone", result);
        return result;
    }

    public Integer getComputer(String hostname) throws RemoteException {
        logRemoteMethodStartCall("getComputer");
        Integer result = target.getComputer(hostname);
        logRemoteMethodEndCall("getComputer", result);
        return result;
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException {
        return target.getDaemonTasks(compId);
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

    public int generateNewID()  throws RemoteException {
        return target.generateNewID();
    }

    public void ping() throws RemoteException {
        target.ping();
    }

    @Override
    public List<String> authenticateUser(String userName, String password) throws RemoteException {
        logRemoteMethodStartCall("authenticateUser");
        List<String> result = target.authenticateUser(userName, password);
        logRemoteMethodEndCall("authenticateUser", result);
        return result;
    }

    @Override
    public VMOptions getClientVMOptions() throws RemoteException {
        logRemoteMethodStartCall("getClientVMOptions");
        VMOptions result = target.getClientVMOptions();
        logRemoteMethodEndCall("getClientVMOptions", result);
        return result;
    }

    public int generateID() throws RemoteException {
        logRemoteMethodStartCall("getUserInfo");
        int result = target.generateID();
        logRemoteMethodEndCall("getUserInfo", result);
        return result;
    }

    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        logRemoteMethodStartCall("remindPassword");
        target.remindPassword(email, localeLanguage);
        logRemoteMethodEndVoidCall("remindPassword");
    }

    public byte[] readFile(String canonicalName, String... params) throws RemoteException {
        logRemoteMethodStartCall("readFile");
        byte[] result = target.readFile(canonicalName, params);
        logRemoteMethodEndVoidCall("readFile");
        return result;
    }

    @Override
    public void runAction(String sid, String... params) throws RemoteException {
        logRemoteMethodStartCall("runAction");
        target.runAction(sid, params);
        logRemoteMethodEndVoidCall("runAction");
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

    @Override
    public boolean checkPropertyChangePermission(String userName, String propertySID) throws RemoteException {
        logRemoteMethodStartCall("checkPropertyChangePermission");
        boolean result = target.checkPropertyChangePermission(userName, propertySID);
        logRemoteMethodEndVoidCall("checkPropertyChangePermission");
        return result;
    }

    public boolean checkFormExportPermission(String canonicalName) throws RemoteException {
        logRemoteMethodStartCall("checkFormExportPermission");
        boolean result = target.checkFormExportPermission(canonicalName);
        logRemoteMethodEndVoidCall("checkFormExportPermission");
        return result;
    }

    public String  addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        logRemoteMethodStartCall("addUser");
        String result = target.addUser(username, email, password, firstName, lastName, localeLanguage);
        logRemoteMethodEndVoidCall("addUser");
        return result;
    }
}
