package platform.client.exceptions;

public class ExceptionThreadGroup extends ThreadGroup {

    public ExceptionThreadGroup() {
        super("Exception thread group");
    }

    public void uncaughtException(Thread t, Throwable e) {
        ClientExceptionManager.handle(e);
    }
}
