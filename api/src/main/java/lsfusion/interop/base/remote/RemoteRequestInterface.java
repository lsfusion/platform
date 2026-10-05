package lsfusion.interop.base.remote;

import lsfusion.interop.action.ServerResponse;

import java.rmi.RemoteException;

public interface RemoteRequestInterface extends PendingRemoteInterface {

    ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object actionResult) throws RemoteException;

    ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException;

    boolean isInServerInvocation(long requestIndex) throws RemoteException;

    // JS controller (CUSTOM / INTERNAL CLIENT) exec(action) / eval(script) / change(property) — run in this request
    // object's context (form: persistent form session; navigator: a fresh session per call). params/value are
    // JSON-decoded canonical values (String/Number/Boolean/null/FileData; dates as offset-bearing ISO strings),
    // bound positionally per interface via Type.parseJSON. The result/exception is delivered by a terminal
    // ControllerCallbackClientAction keyed by requestIndex. See GFORM-CONTROLLER-EXEC-EVAL-PLAN §5/§12.

    ServerResponse exec(long requestIndex, long lastReceivedRequestIndex, String action, Object[] params) throws RemoteException;

    // evalAction toggles how the script is parsed: true -> the script is an action body, auto-wrapped into a
    // run(...) action (EVAL ACTION / HTTP /eval/action); false -> the script defines its own run action and can
    // declare typed parameters (EVAL / HTTP /eval).
    ServerResponse eval(long requestIndex, long lastReceivedRequestIndex, String script, boolean evalAction, Object[] params) throws RemoteException;

    ServerResponse change(long requestIndex, long lastReceivedRequestIndex, String property, Object[] params, Object value) throws RemoteException;

    // a part of the file the server left for the client to read (see WriteServerFileClientAction) - it is read part by part,
    // because the whole file can be bigger than the max java array (2GB) and the memory of the servers / client.
    // A chunk shorter than length is the last one
    byte[] readFileChunk(String fileId, long offset, int length) throws RemoteException;
}
