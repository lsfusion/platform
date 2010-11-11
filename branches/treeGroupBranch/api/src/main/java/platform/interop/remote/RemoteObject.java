package platform.interop.remote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteObject extends UnicastRemoteObject implements PendingRemote {

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
        for (MethodInvocation invocation : invocations) {
            try {
                result = invoke(this, invocation);
            } catch (InvocationTargetException e) {
                throw new RemoteException("Ошибка при вызове метода: " + invocation.name, e.getTargetException());
            } catch (Exception e) {
                throw new RemoteException("Ошибка при вызове метода: " + invocation.name, e);
            }
        }

        return result;
    }

    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException {
        if (invocations == null) {
            invocations = new MethodInvocation[0];
        }

        Object createdObject = null;
        try {
            createdObject = invoke(this, creator);
        } catch (Exception e) {
            throw new RemoteException("Не могу создать объект через вызов метода: " + creator.toString());
        }

        Object[] result = new Object[invocations.length + 1];
        result[0] = createdObject;
        for (int i = 0; i < invocations.length; ++i) {
            try {
                result[i+1] = invoke(createdObject, invocations[i]);
            } catch (InvocationTargetException e) {
                throw new RemoteException("Ошибка при вызове метода: " + invocations[i].name, e.getTargetException());
            } catch (Exception e) {
                throw new RemoteException("Ошибка при вызове метода: " + invocations[i].name, e);
            }
        }
        
        return result;
    }

    private Object invoke(Object target, MethodInvocation invocation) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = target.getClass().getMethod(invocation.name, invocation.params);
        return method.invoke(target, invocation.args);
    }
}
