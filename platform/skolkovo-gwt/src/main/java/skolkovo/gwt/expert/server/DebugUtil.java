package skolkovo.gwt.expert.server;

import java.rmi.RemoteException;

public class DebugUtil {
    public static Throwable getInitialCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result instanceof RemoteException) {
            result = result.getCause();
        }

        return result;
    }
}
