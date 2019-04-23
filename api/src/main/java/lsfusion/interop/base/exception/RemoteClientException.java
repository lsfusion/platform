package lsfusion.interop.base.exception;

// ошибка связи
public abstract class RemoteClientException extends RemoteHandledException {

    public final long reqId;

    public RemoteClientException(String message, long reqId) {
        super(message);

        this.reqId = reqId;
    }
}
