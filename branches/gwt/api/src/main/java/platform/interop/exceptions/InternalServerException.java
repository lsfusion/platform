package platform.interop.exceptions;

public class InternalServerException extends RemoteServerException {
    public String trace;

    public InternalServerException(int ID, String message, String trace) {
        super("Внутренняя ошибка сервера номер " + ID + " : " + message);
        this.trace = trace;
    }
}
