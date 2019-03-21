package lsfusion.interop.base.exception;

public class FatalRemoteClientException extends RemoteClientException {
    
    public FatalRemoteClientException(String message, long reqId) {
        super(message, reqId);
    }
}
