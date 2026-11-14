package lsfusion.interop.base.remote;

import lsfusion.interop.action.ServerResponse;

import java.rmi.RemoteException;

public interface RemoteRequestInterface extends PendingRemoteInterface {

    ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object actionResult) throws RemoteException;

    ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException;

    boolean isInServerInvocation(long requestIndex) throws RemoteException;

    // JS controller (CUSTOM / INTERNAL CLIENT) exec(action) / eval(script) / change(property) — run in this request
    // object's context (form: persistent form session; navigator: a fresh session per call). params/value are
    // canonical values - a Double, a Boolean, null, and for anything else its JSON text (a string in its quotes, a date
    // as an offset-bearing ISO string, an object, an array) -, bound positionally per interface via Type.parseJSON; a
    // file is given as a string, its text, or as an object that describes it. The result/exception is delivered by a terminal
    // ControllerCallbackClientAction keyed by requestIndex. See GFORM-CONTROLLER-EXEC-EVAL-PLAN §5/§12.
    // options are the options of the call: a JSON object's text, null for none. mcp: true (controller.mcp in JS) makes
    // the call as the MCP eval tool's: params is one string, the JSON text of the tool's params, bound as the tool binds
    // them, and the result is what the tool answers - {results, status}.

    ServerResponse exec(long requestIndex, long lastReceivedRequestIndex, String action, Object[] params, String options) throws RemoteException;

    // evalAction toggles how the script is parsed: true -> the script is an action body, auto-wrapped into a
    // run(...) action (EVAL ACTION / HTTP /eval/action); false -> the script defines its own run action and can
    // declare typed parameters (EVAL / HTTP /eval).
    ServerResponse eval(long requestIndex, long lastReceivedRequestIndex, String script, boolean evalAction, Object[] params, String options) throws RemoteException;

    ServerResponse change(long requestIndex, long lastReceivedRequestIndex, String property, Object[] params, Object value) throws RemoteException;

    // a part of the file the server left for the client to read (see WriteServerFileClientAction) - it is read part by part,
    // because the whole file can be bigger than the max java array (2GB) and the memory of the servers / client.
    // A chunk shorter than length is the last one
    byte[] readFileChunk(String fileId, long offset, int length) throws RemoteException;
}
