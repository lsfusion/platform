package lsfusion.interop.exceptions;

// Rmi-запрос может "зависнуть", соотвественно скажем после рестарта клиента, можно получмть Interrupted, который клиент скорее всего проигнорирует, и мы не хотим видеть эти сообшения в логе
public class RemoteInterruptedException extends RemoteServerException {

    public RemoteInterruptedException(Throwable cause) {
        super(cause);
    }
}
