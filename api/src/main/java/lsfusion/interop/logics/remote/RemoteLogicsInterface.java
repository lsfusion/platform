package lsfusion.interop.logics.remote;

import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.authentication.Authentication;
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
    AuthenticationToken authenticateUser(Authentication authentication) throws RemoteException;

    // stateful interfaces
    RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) throws RemoteException;
    RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException;

    // RESTful interfaces
    // external requests (interface is similar to RemoteSessionInterface but with token)
    ExternalResponse exec(AuthenticationToken token, ConnectionInfo connectionInfo, String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(AuthenticationToken token, ConnectionInfo connectionInfo, boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException;

    // MCP (Model Context Protocol) — JSON-RPC 2.0 dispatch (initialize / tools/list / tools/call).
    // The body is the raw JSON-RPC request; the return value is the raw JSON-RPC response, or
    // null for JSON-RPC notifications (requests without an `id`). Empty body is treated as a
    // `tools/list` request, useful for quick discovery.
    // The auth context (token + connectionInfo) flows through to tools that touch session state,
    // so e.g. `lsfusion_eval` runs under the caller's identity instead of always anonymous.
    // The `request` envelope carries the inbound HTTP request's metadata (headers, cookies,
    // scheme, host, contextPath, sessionId, etc.) — same shape /eval already provides — so
    // scripts running through `lsfusion_eval` can read those attributes via standard lsFusion
    // properties (`headers[name]`, `cookies[name]`, etc.). Its `params`/`returnNames` are
    // irrelevant — eval supplies its own from tool args.
    String mcp(AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request, String body) throws RemoteException;

    // separate methods, because used really often (and don't need authentication)
    long generateID() throws RemoteException;
    void ping() throws RemoteException;
    void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) throws RemoteException;
    byte[] findClass(String name) throws RemoteException;

    List<String> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;

    void registerClient(RemoteClientInterface client) throws RemoteException;
}
