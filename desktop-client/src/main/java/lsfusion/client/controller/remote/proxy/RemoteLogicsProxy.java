package lsfusion.client.controller.remote.proxy;

import lsfusion.client.navigator.controller.remote.proxy.RemoteNavigatorProxy;
import lsfusion.client.session.remote.proxy.RemoteSessionProxy;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.session.remote.RemoteSessionInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class RemoteLogicsProxy<T extends RemoteLogicsInterface> extends PendingRemoteObjectProxy<T> implements RemoteLogicsInterface {

    public RemoteLogicsProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    public RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) throws RemoteException {
        return new RemoteNavigatorProxy(target.createNavigator(token, navigatorInfo), realHostName);
    }

    @Override
    public RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException {
        return new RemoteSessionProxy<>(target.createSession(token, sessionInfo), realHostName);
    }

    public void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap)  throws RemoteException {
        target.sendPingInfo(computerName, pingInfoMap);
    }

    public void ping() throws RemoteException {
        target.ping();
    }

    public byte[] findClass(String name) throws RemoteException {
        return target.findClass(name);
    }

    @Override
    public AuthenticationToken authenticateUser(String userName, String password) throws RemoteException {
        logRemoteMethodStartCall("authenticateUser");
        AuthenticationToken result = target.authenticateUser(userName, password);
        logRemoteMethodEndCall("authenticateUser", result);
        return result;
    }

    @Override
    public AuthenticationToken oAuth2authenticateUser(String userName) throws RemoteException {
        logRemoteMethodStartCall("oAuth2authenticateUser");
        AuthenticationToken result = target.oAuth2authenticateUser(userName);
        logRemoteMethodEndCall("oAuth2authenticateUser", result);
        return result;
    }

    public long generateID() throws RemoteException {
        logRemoteMethodStartCall("getUserInfo");
        long result = target.generateID();
        logRemoteMethodEndCall("getUserInfo", result);
        return result;
    }

    @Override
    public ExternalResponse exec(AuthenticationToken token, SessionInfo sessionInfo, String action, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("exec");
        ExternalResponse result = target.exec(token, sessionInfo, action, request);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ExternalResponse eval(AuthenticationToken token, SessionInfo sessionInfo, boolean action, Object paramScript, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("eval");
        ExternalResponse result = target.eval(token, sessionInfo, action, paramScript, request);
        logRemoteMethodEndVoidCall("eval");
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
