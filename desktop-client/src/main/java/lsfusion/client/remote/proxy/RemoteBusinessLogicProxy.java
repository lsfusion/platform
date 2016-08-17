package lsfusion.client.remote.proxy;

import lsfusion.base.NavigatorInfo;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.VMOptions;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface> extends RemoteObjectProxy<T> implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException {
        RemoteNavigatorInterface remote = target.createNavigator(isFullClient, navigatorInfo, forceCreateNew);
        if (remote == null) {
            return null;
        }
        return new RemoteNavigatorProxy(remote);
    }

    @Override
    public Integer getApiVersion() throws RemoteException {
        return target.getApiVersion();
    }

    public GUIPreferences getGUIPreferences() throws RemoteException {
        logRemoteMethodStartCall("getGUIPreferences");
        GUIPreferences result = target.getGUIPreferences();
        logRemoteMethodEndCall("getGUIPreferences", result);
        return result;    
    }

    @Override
    public LocalePreferences getDefaultLocalePreferences() throws RemoteException {
        logRemoteMethodStartCall("getDefaultLocalePreferences");
        LocalePreferences result = target.getDefaultLocalePreferences();
        logRemoteMethodEndCall("getDefaultLocalePreferences", result);
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

    public void sendPingInfo(Integer computerId, Map<Long, List<Long>> pingInfoMap)  throws RemoteException {
        target.sendPingInfo(computerId, pingInfoMap);
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

    @Override
    public boolean isSingleInstance() throws RemoteException {
        logRemoteMethodStartCall("isSingleInstance");
        boolean result = target.isSingleInstance();
        logRemoteMethodEndVoidCall("isSingleInstance");
        return result;
    }

    @Override
    public boolean isBusyDialog() throws RemoteException {
        return target.isBusyDialog();
    }

    public String  addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        logRemoteMethodStartCall("addUser");
        String result = target.addUser(username, email, password, firstName, lastName, localeLanguage);
        logRemoteMethodEndVoidCall("addUser");
        return result;
    }
}
