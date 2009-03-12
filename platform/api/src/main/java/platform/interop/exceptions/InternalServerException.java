package platform.interop.exceptions;

public class InternalServerException extends RemoteServerException {

    public InternalServerException(int ID, String message) {
        super("Внутренняя ошибка сервера номер " + ID + " : " + message);
    }
}
