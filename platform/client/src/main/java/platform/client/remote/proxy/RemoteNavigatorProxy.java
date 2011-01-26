package platform.client.remote.proxy;

import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.List;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface>
        extends RemoteObjectProxy<T>
        implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public String getForms(String formSet) throws RemoteException {
        logRemoteMethodStartCall("getForms");
        String result = target.getForms(formSet);
        logRemoteMethodEndCall("getForms", result);
        return result;
    }

    @NonPendingRemoteMethod
    public RemoteFormInterface createForm(String formSID, boolean currentSession) throws RemoteException {
        List<MethodInvocation> invocations = getImmutableMethodInvocations(RemoteFormProxy.class);

        boolean hasCachedRichDesignByteArray = false;
        if (RemoteFormProxy.cachedRichDesign.get(formSID) != null) {
            hasCachedRichDesignByteArray = true;
            for (int i = 0; i < invocations.size(); ++i) {
                if (invocations.get(i).name.equals("getRichDesignByteArray")) {
                    invocations.remove(i);
                    break;
                }
            }
        }

        RemoteFormProxy proxy = createForm(invocations, MethodInvocation.create(this.getClass(), "createForm", formSID, currentSession));

        if (hasCachedRichDesignByteArray) {
            proxy.setProperty("getRichDesignByteArray", RemoteFormProxy.cachedRichDesign.get(formSID));
        } else {
            RemoteFormProxy.cachedRichDesign.put(formSID, (byte[]) proxy.getProperty("getRichDesignByteArray"));
        }

        return proxy;
    }

    @NonPendingRemoteMethod
    public RemoteFormInterface createForm(byte[] formState) throws RemoteException {
        return createForm(getImmutableMethodInvocations(RemoteFormProxy.class),
                MethodInvocation.create(this.getClass(), "createForm", formState));
    }

    @NonFlushRemoteMethod
    private RemoteFormProxy createForm(List<MethodInvocation> invocations, MethodInvocation creator) throws RemoteException {

        Object[] result = createAndExecute(creator, invocations.toArray(new MethodInvocation[invocations.size()]));

        RemoteFormInterface remoteForm = (RemoteFormInterface) result[0];
        if (remoteForm == null) {
            return null;
        }

        RemoteFormProxy proxy = new RemoteFormProxy(remoteForm);
        for (int i = 0; i < invocations.size(); ++i) {
            proxy.setProperty(invocations.get(i).name, result[i + 1]);
        }

        return proxy;
    }

    public void saveForm(String formSID, byte[] formState) throws RemoteException {
        target.saveForm(formSID, formState);
    }

    public void saveVisualSetup(byte[] data) throws RemoteException {
        target.saveVisualSetup(data);
    }

    public byte[] getRichDesignByteArray(String formSID) throws RemoteException {
        return target.getRichDesignByteArray(formSID);
    }

    public byte[] getFormEntityByteArray(String formSID) throws RemoteException {
        return target.getFormEntityByteArray(formSID);
    }

    public byte[] getCurrentUserInfoByteArray() throws RemoteException {
        logRemoteMethodStartCall("getCurrentUserInfoByteArray");
        byte[] result = target.getCurrentUserInfoByteArray();
        logRemoteMethodEndCall("getCurrentUserInfoByteArray", result);
        return result;
    }

    public byte[] getElementsByteArray(String groupSID) throws RemoteException {
        logRemoteMethodStartCall("getElementsByteArray");
        byte[] result = target.getElementsByteArray(groupSID);
        logRemoteMethodEndCall("getElementsByteArray", result);
        return result;
    }

    public void relogin(String login) throws RemoteException {
        target.relogin(login);
    }

    public void clientExceptionLog(String info) throws RemoteException {
        target.clientExceptionLog(info);
    }

    public void close() throws RemoteException {
        target.close();
    }

    public void denyRestart() throws RemoteException {
        target.denyRestart();
    }
}
