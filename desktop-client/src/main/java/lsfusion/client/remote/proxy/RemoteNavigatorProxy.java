package lsfusion.client.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.base.DefaultForms;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface>
        extends RemoteObjectProxy<T>
        implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) throws RemoteException {
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

        RemoteFormProxy proxy = createForm(invocations, MethodInvocation.create(this.getClass(), "createForm", formSID, initialObjects, isModal, interactive));

        if (hasCachedRichDesignByteArray) {
            proxy.setProperty("getRichDesignByteArray", RemoteFormProxy.cachedRichDesign.get(formSID));
        } else {
            RemoteFormProxy.cachedRichDesign.put(formSID, (byte[]) proxy.getProperty("getRichDesignByteArray"));
        }

        return proxy;
    }

    public RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException {
        return createForm(getImmutableMethodInvocations(RemoteFormProxy.class),
                          MethodInvocation.create(this.getClass(), "createPreviewForm", new Object[]{formState}));
    }

    private RemoteFormProxy createForm(List<MethodInvocation> immutableMethods, MethodInvocation creator) throws RemoteException {

        Object[] result = createAndExecute(creator, immutableMethods.toArray(new MethodInvocation[immutableMethods.size()]));

        RemoteFormInterface remoteForm = (RemoteFormInterface) result[0];
        if (remoteForm == null) {
            return null;
        }

        RemoteFormProxy proxy = new RemoteFormProxy(remoteForm);
        for (int i = 0; i < immutableMethods.size(); ++i) {
            proxy.setProperty(immutableMethods.get(i).name, result[i + 1]);
        }

        return proxy;
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

    public void logClientException(String info, String client, String message, String type, String erTrace) throws RemoteException {
        target.logClientException(info, client, message, type, erTrace);
    }

    public void close() throws RemoteException {
        target.close();
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    public void setUpdateTime(int updateTime) throws RemoteException {
        target.setUpdateTime(updateTime);
    }

    @ImmutableMethod
    public DefaultForms showDefaultForms() throws RemoteException {
        try {
            return callImmutableMethod("showDefaultForms", new Callable<DefaultForms>() {
                @Override
                public DefaultForms call() throws Exception {
                    return target.showDefaultForms();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public List<String> getDefaultForms() throws RemoteException {
        try {
            return callImmutableMethod("getDefaultForms", new Callable<List<String>>() {
                @Override
                public List<String> call() throws Exception {
                    return target.getDefaultForms();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public byte[] getNavigatorTree() throws RemoteException {
        try {
            return callImmutableMethod("getNavigatorTree", new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    return target.getNavigatorTree();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public byte[] getCommonWindows() throws RemoteException {
        try {
            return callImmutableMethod("getCommonWindows", new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    return target.getCommonWindows();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String getCurrentFormSID() throws RemoteException {
        return target.getCurrentFormSID();
    }

    @Override
    public boolean isConfigurationAccessAllowed() throws RemoteException {
        return target.isConfigurationAccessAllowed();
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException {
        return target.executeNavigatorAction(navigatorActionSID);
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return target.continueNavigatorAction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Exception clientException) throws RemoteException {
        return target.throwInNavigatorAction(clientException);
    }
}
