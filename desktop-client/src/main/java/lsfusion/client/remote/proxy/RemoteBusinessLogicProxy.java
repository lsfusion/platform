package lsfusion.client.remote.proxy;

import lsfusion.base.NavigatorInfo;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PreAuthentication;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface> extends RemoteObjectProxy<T> implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean forceCreateNew) throws RemoteException {
        return new RemoteNavigatorProxy(target.createNavigator(isFullClient, navigatorInfo, forceCreateNew));
    }

    @Override
    public Integer getApiVersion() throws RemoteException {
        return target.getApiVersion();
    }

    @Override
    public String getPlatformVersion() throws RemoteException {
        return target.getPlatformVersion();
    }

    public GUIPreferences getGUIPreferences() throws RemoteException {
        logRemoteMethodStartCall("getGUIPreferences");
        GUIPreferences result = target.getGUIPreferences();
        logRemoteMethodEndCall("getGUIPreferences", result);
        return result;    
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(long compId) throws RemoteException {
        return target.getDaemonTasks(compId);
    }

    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreen");
        ExternalScreen result = target.getExternalScreen(screenID);
        logRemoteMethodEndCall("getExternalScreen", result);
        return result;        
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, long computerId) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreenParameters");
        ExternalScreenParameters result = target.getExternalScreenParameters(screenID, computerId);
        logRemoteMethodEndCall("getExternalScreenParameters", result);
        return result;
    }

    public void sendPingInfo(Long computerId, Map<Long, List<Long>> pingInfoMap)  throws RemoteException {
        target.sendPingInfo(computerId, pingInfoMap);
    }

    public void ping() throws RemoteException {
        target.ping();
    }

    @Override
    public PreAuthentication preAuthenticateUser(String userName, String password, String language, String country) throws RemoteException {
        logRemoteMethodStartCall("authenticateUser");
        PreAuthentication result = target.preAuthenticateUser(userName, password, language, country);
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

    public long generateID() throws RemoteException {
        logRemoteMethodStartCall("getUserInfo");
        long result = target.generateID();
        logRemoteMethodEndCall("getUserInfo", result);
        return result;
    }

    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        logRemoteMethodStartCall("remindPassword");
        target.remindPassword(email, localeLanguage);
        logRemoteMethodEndVoidCall("remindPassword");
    }

    @Override
    public List<Object> exec(String action, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[][] headerValues) throws RemoteException {
        logRemoteMethodStartCall("exec");
        List<Object> result = target.exec(action, returnCanonicalNames, params, charset, headerNames, headerValues);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public List<Object> eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[][] headerValues) throws RemoteException {
        logRemoteMethodStartCall("eval");
        List<Object> result = target.eval(action, paramScript, returnCanonicalNames, params, charset, headerNames, headerValues);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }

    @Override
    public boolean isSingleInstance() throws RemoteException {
        logRemoteMethodStartCall("isSingleInstance");
        boolean result = target.isSingleInstance();
        logRemoteMethodEndVoidCall("isSingleInstance");
        return result;
    }

    public String  addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        logRemoteMethodStartCall("addUser");
        String result = target.addUser(username, email, password, firstName, lastName, localeLanguage);
        logRemoteMethodEndVoidCall("addUser");
        return result;
    }

    @Override
    public Map<String, String> readMemoryLimits() throws RemoteException {
        logRemoteMethodStartCall("readMemoryLimits");
        Map<String, String> result = target.readMemoryLimits();
        logRemoteMethodEndVoidCall("readMemoryLimits");
        return result;
    }

    @Override
    public List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException {
        logRemoteMethodStartVoidCall("saveCustomReportPathList");
        List<ReportPath> result = target.saveAndGetCustomReportPathList(formSID, recreate);
        logRemoteMethodEndVoidCall("saveCustomReportPathList");
        return result;
    }
}
