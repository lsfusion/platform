package lsfusion.client.remote.proxy;

import lsfusion.base.ExternalRequest;
import lsfusion.base.ExternalResponse;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SessionInfo;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.AuthenticationToken;
import lsfusion.interop.session.RemoteSessionInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class RemoteLogicsProxy<T extends RemoteLogicsInterface> extends RemoteObjectProxy<T> implements RemoteLogicsInterface {

    public RemoteLogicsProxy(T target) {
        super(target);
    }

    public RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) throws RemoteException {
        return new RemoteNavigatorProxy(target.createNavigator(token, navigatorInfo));
    }

    @Override
    public RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException {
        return new RemoteSessionProxy<>(target.createSession(token, sessionInfo));
    }

    public void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap)  throws RemoteException {
        target.sendPingInfo(computerName, pingInfoMap);
    }

    public void ping() throws RemoteException {
        target.ping();
    }

    @Override
    public AuthenticationToken authenticateUser(String userName, String password) throws RemoteException {
        logRemoteMethodStartCall("authenticateUser");
        AuthenticationToken result = target.authenticateUser(userName, password);
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
