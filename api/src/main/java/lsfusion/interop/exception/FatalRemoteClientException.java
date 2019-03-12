package lsfusion.interop.exception;

public class FatalRemoteClientException extends RemoteClientException {
    
    public FatalRemoteClientException(String message, long reqId) {
        super(message, reqId);
    }
}
