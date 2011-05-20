package skolkovo.gwt.base.server;

import java.rmi.RemoteException;

public class DebugUtil {
    public static Throwable getInitialCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null && result instanceof RemoteException) {
            result = result.getCause();
        }

        return result;
    }
}
