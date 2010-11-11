package platform.interop.exceptions;

public class LoginException extends RemoteServerException {

    public LoginException() {
        super("Неправильное имя пользователя или пароль");
    }
}
