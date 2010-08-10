package platform.interop.remote;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteObject extends UnicastRemoteObject implements PendingRemote {
    private static Logger logger = Logger.getLogger(RemoteObject.class.getName());

    final protected int exportPort;

    public int getExportPort() {
        return exportPort;
    }

    public RemoteObject(int port) throws RemoteException {
        super(port);

        exportPort = port;
    }

    public Object execute(MethodInvocation[] invocations) throws RemoteException {
        Object result = null;
        Class thisClass = this.getClass();
        for (MethodInvocation invocation : invocations) {
            try {
                Method method = thisClass.getMethod(invocation.name, invocation.params);
                result = method.invoke(this, invocation.args);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Ошибка при вызове метода: " + invocation.name, e);
            }
        }

        return result;
    }
}
