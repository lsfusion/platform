package platform.interop.exceptions;

public class ComplexQueryException extends RemoteServerException {

    public ComplexQueryException() {
        super("Слишком сложный запрос к базе данных");
    }
}
