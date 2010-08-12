package platform.client.remote.proxy;

import platform.interop.RemoteLogicsInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.List;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface>
        extends RemoteObjectProxy<T>
        implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    public String getName() throws RemoteException {
        logRemoteMethodCall("getName");
        return target.getName();
    }

    public byte[] findClass(String name) throws RemoteException {
        logRemoteMethodCall("findClass");
        return target.findClass(name);
    }

    @NonPendingRemoteMethod
    public RemoteNavigatorInterface createNavigator(String login, String password, int computer) throws RemoteException {
        List<MethodInvocation> invocations = getImmutableMethodInvocations(RemoteNavigatorProxy.class);

        MethodInvocation creator = MethodInvocation.create(this.getClass(), "createNavigator", login, password, computer);

        Object[] result = createAndExecute(creator, invocations.toArray(new MethodInvocation[invocations.size()]));

        RemoteNavigatorInterface remoteDialog = (RemoteNavigatorInterface) result[0];
        RemoteNavigatorProxy proxy = new RemoteNavigatorProxy(remoteDialog);
        for (int i = 0; i < invocations.size(); ++i) {
            proxy.setProperty(invocations.get(i).name, result[i+1]);
        }

        return proxy;
    }

    public Integer getComputer(String hostname) throws RemoteException {
        logRemoteMethodCall("getComputer");
        return target.getComputer(hostname);
    }

    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        logRemoteMethodCall("getExternalScreen");
        return target.getExternalScreen(screenID);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        logRemoteMethodCall("getExternalScreenParameters");
        return target.getExternalScreenParameters(screenID, computerId);
    }
}
