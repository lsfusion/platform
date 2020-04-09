package lsfusion.server.base.controller.remote;

import lsfusion.interop.PendingMethodInvocation;
import lsfusion.interop.base.remote.PendingRemoteInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.List;

import static lsfusion.base.ApiResourceBundle.getString;

public abstract class PendingRemoteObject extends RemoteObject implements PendingRemoteInterface {

    public PendingRemoteObject() {
        super();
    }

    public PendingRemoteObject(int port) throws RemoteException {
        super(port, true);
    }

    public PendingRemoteObject(int port, boolean autoExport) throws RemoteException {
        super(port, autoExport);
    }

    public Object[] createAndExecute(PendingMethodInvocation creator, PendingMethodInvocation[] invocations) throws RemoteException {
        if (invocations == null) {
            invocations = new PendingMethodInvocation[0];
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

    private Object invoke(Object target, PendingMethodInvocation invocation) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = target.getClass().getMethod(invocation.name, invocation.params);
        return method.invoke(target, invocation.args);
    }

    public String getRemoteActionMessage() throws RemoteException {
        return null;
    }
    public List<Object> getRemoteActionMessageList() throws RemoteException {
        return null;
    }

    @Override
    public void interrupt(boolean cancelable) throws RemoteException {
    }
}
