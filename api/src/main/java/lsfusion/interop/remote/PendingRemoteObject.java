package lsfusion.interop.remote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import static lsfusion.base.ApiResourceBundle.getString;

public class PendingRemoteObject extends RemoteObject implements PendingRemoteInterface {

    public PendingRemoteObject() {
        super();
    }

    public PendingRemoteObject(int port) throws RemoteException {
        super(port, true);
    }

    public PendingRemoteObject(int port, boolean autoExport) throws RemoteException {
        super(port, autoExport);
    }

    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException {
        if (invocations == null) {
            invocations = new MethodInvocation[0];
        }

        Object createdObject = null;
        try {
            createdObject = invoke(this, creator);
        } catch (Exception e) {
            throw new RuntimeException(getString("remote.can.not.create.object.by.calling.method", creator.toString()), e);
        }

        Object[] result = new Object[invocations.length + 1];
        result[0] = createdObject;
        for (int i = 0; i < invocations.length; ++i) {
            try {
                result[i+1] = createdObject == null ? null : invoke(createdObject, invocations[i]);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(getString("remote.error.calling.method", invocations[i].name), e.getTargetException());
            } catch (Exception e) {
                throw new RuntimeException(getString("remote.error.calling.method", invocations[i].name), e);
            }
        }

        return result;
    }

    private Object invoke(Object target, MethodInvocation invocation) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = target.getClass().getMethod(invocation.name, invocation.params);
        return method.invoke(target, invocation.args);
    }

    public String getRemoteActionMessage() throws RemoteException {
        return null;
    }
}
