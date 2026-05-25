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
    // The body is the raw JSON-RPC request; the return value pairs the JSON-RPC response with a
    // side-map of binary payloads that didn't fit inline (the web tier resolves placeholders to
    // download URLs before writing the response). {@code MCPResult.json == null} signals a
    // JSON-RPC notification (request without `id`); web tier replies with HTTP 202.
    // Empty body is treated as a `tools/list` request, useful for quick discovery.
    // The auth context (token + connectionInfo) flows through to tools that touch session state,
    // so e.g. `lsfusion_eval` runs under the caller's identity instead of always anonymous.
    // The `request` envelope carries the inbound HTTP request's metadata (headers, cookies,
    // scheme, host, contextPath, sessionId, etc.) — same shape /eval already provides — so
    // scripts running through `lsfusion_eval` can read those attributes via standard lsFusion
    // properties (`headers[name]`, `cookies[name]`, etc.). Its `params`/`returnNames` are
    // irrelevant — eval supplies its own from tool args.
    MCPResult mcp(AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request, String body) throws RemoteException;

    // Plain-HTTP sibling of the MCP file tools — backs the /files/{list,search,read} endpoints.
    // Symmetric with eval: where /eval runs an lsf action under the caller's auth, /files browses
    // the server classpath (the same code MCP's lsfusion_files_* tools run), so HTTP callers get
    // the file tools without speaking JSON-RPC. {@code operation} is the bare verb
    // ({@code list} / {@code search} / {@code read}); {@code argsJson} is the JSON arguments object
    // (same shape as the matching MCP tool's inputSchema — empty / null ⇒ no args). Runs the same
    // per-role {@code enableAPI} gate as the MCP file tools, then returns the raw MCPFileTools
    // payload as a JSON string — no JSON-RPC envelope, no binary slimming (a read of a binary
    // resource returns {@code contentBase64} inline). The {@code request} envelope carries the
    // inbound HTTP request's metadata, same as {@link #mcp} / {@code /eval}.
    String files(AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request, String operation, String argsJson) throws RemoteException;

    // OAuth Authorization Server — single dispatch method for all OAuth-server operations.
    // {@code operation} is one of the wire-stable strings declared as constants in
    // {@code OAuthOperations} ({@code registerClient}, {@code getClient},
    // {@code issueAuthCode}, {@code exchangeCode}, {@code refreshToken},
    // {@code revokeToken}, {@code validateToken}). {@code requestJson} is the
    // JSON-encoded operation-specific request body. The return value is a JSON-encoded
    // operation-specific response,
    // or an RFC 6749 §5.2 error envelope ({@code {"error": ..., "error_description": ...}})
    // on failure. {@code token} is the caller's auth context: most operations are
    // anonymous (the OAuth protocol itself doesn't authenticate the requester at the
    // protocol level — it operates *to produce* an authentication), but a few (e.g.
    // {@code issueAuthCode}, which runs after a user logged in via /oauth/authorize)
    // need the user identity from the web tier's existing security context.
    String oauth(AuthenticationToken token, String operation, String requestJson) throws RemoteException;

    // separate methods, because used really often (and don't need authentication)
    long generateID() throws RemoteException;
    void ping() throws RemoteException;
    void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) throws RemoteException;
    byte[] findClass(String name) throws RemoteException;

    List<String> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;

    void registerClient(RemoteClientInterface client) throws RemoteException;
}
