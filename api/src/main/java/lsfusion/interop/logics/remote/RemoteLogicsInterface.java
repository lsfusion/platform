package lsfusion.interop.logics.remote;

import lsfusion.base.Pair;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.PassObject;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.session.remote.RemoteSessionInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    // obsolete

    // main interface

    // authentication
    AuthenticationToken authenticateUser(String userName, PassObject passObject) throws RemoteException;
    Pair<String, String> getOauth2ClientCredentials(String client, String authSecret) throws RemoteException;

    // stateful interfaces
    RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) throws RemoteException;
    RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException;

    // RESTful interfaces
    // external requests (interface is similar to RemoteSessionInterface but with token)
    ExternalResponse exec(AuthenticationToken token, SessionInfo sessionInfo, String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(AuthenticationToken token, SessionInfo sessionInfo, boolean action, Object paramScript, ExternalRequest request) throws RemoteException;

    // separate methods, because used really often (and don't need authentication)
    long generateID() throws RemoteException;
    void ping() throws RemoteException;
    void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) throws RemoteException;
    byte[] findClass(String name) throws RemoteException;

    List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;
}
