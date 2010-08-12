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
        logRemoteMethodCall("getForms");
        return target.getForms(formSet);
    }

    @NonPendingRemoteMethod
    public RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException {
        List<MethodInvocation> invocations = getImmutableMethodInvocations(RemoteFormProxy.class);

        boolean hasCachedRichDisignByteArray = false;
        if (RemoteFormProxy.cachedRichDesign.get(formID) != null) {
            hasCachedRichDisignByteArray = true;
            for (int i = 0; i < invocations.size(); ++i) {
                if (invocations.get(i).name.equals("getRichDesignByteArray")) {
                    invocations.remove(i);
                    break;
                }
            }
        }

        MethodInvocation creator = MethodInvocation.create(this.getClass(), "createForm", formID, currentSession);

        Object[] result = createAndExecute(creator, invocations.toArray(new MethodInvocation[invocations.size()]));

        RemoteFormInterface remoteForm = (RemoteFormInterface) result[0];
        RemoteFormProxy proxy = new RemoteFormProxy(remoteForm);
        for (int i = 0; i < invocations.size(); ++i) {
            proxy.setProperty(invocations.get(i).name, result[i + 1]);
        }

        if (hasCachedRichDisignByteArray) {
            proxy.setProperty("getRichDesignByteArray", RemoteFormProxy.cachedRichDesign.get(formID));
        } else {
            RemoteFormProxy.cachedRichDesign.put(formID, (byte[]) proxy.getProperty("getRichDesignByteArray"));
        }

        return proxy;
    }

    @ImmutableMethod
    public byte[] getCurrentUserInfoByteArray() throws RemoteException {
        logRemoteMethodCall("getCurrentUserInfoByteArray");
        return target.getCurrentUserInfoByteArray();
    }

    public byte[] getElementsByteArray(int groupID) throws RemoteException {
        logRemoteMethodCall("getElementsByteArray");
        return target.getElementsByteArray(groupID);
    }
}
