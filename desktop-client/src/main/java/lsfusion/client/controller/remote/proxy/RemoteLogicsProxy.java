package lsfusion.client.controller.remote.proxy;

import lsfusion.client.navigator.controller.remote.proxy.RemoteNavigatorProxy;
import lsfusion.client.session.remote.proxy.RemoteSessionProxy;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.authentication.Authentication;
import lsfusion.interop.logics.remote.RemoteClientInterface;
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
    public AuthenticationToken authenticateUser(Authentication authentication) throws RemoteException {
        logRemoteMethodStartCall("authenticateUser");
        AuthenticationToken result = target.authenticateUser(authentication);
        logRemoteMethodEndCall("authenticateUser", result);
        return result;
    }

    public long generateID() throws RemoteException {
        logRemoteMethodStartCall("getUserInfo");
        long result = target.generateID();
        logRemoteMethodEndCall("getUserInfo", result);
        return result;
    }

    @Override
    public ExternalResponse exec(AuthenticationToken token, ConnectionInfo connectionInfo, String action, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("exec");
        ExternalResponse result = target.exec(token, connectionInfo, action, request);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ExternalResponse eval(AuthenticationToken token, ConnectionInfo connectionInfo, boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("eval");
        ExternalResponse result = target.eval(token, connectionInfo, action, paramScript, request);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }

    @Override
    public List<String> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException {
        logRemoteMethodStartVoidCall("saveCustomReportPathList");
        List<String> result = target.saveAndGetCustomReportPathList(formSID, recreate);
        logRemoteMethodEndVoidCall("saveCustomReportPathList");
        return result;
    }

    @Override
    public void registerClient(RemoteClientInterface client) throws RemoteException {
        logRemoteMethodStartVoidCall("registerClient");
        target.registerClient(client);
        logRemoteMethodEndVoidCall("registerClient");
    }
}
