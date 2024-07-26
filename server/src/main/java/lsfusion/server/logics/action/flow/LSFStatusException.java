package lsfusion.server.logics.action.flow;

public class LSFStatusException extends LSFException {
    public int status;

    public LSFStatusException(String message, int status) {
        super(message);
        this.status = status;
    }
}
