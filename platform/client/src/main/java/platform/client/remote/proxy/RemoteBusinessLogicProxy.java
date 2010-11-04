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

    public String getName() throws RemoteException {
        logRemoteMethodStartCall("getName");
        String result = target.getName();
        logRemoteMethodEndCall("getName", result);
        return result;
    }

    public Integer getComputer(String hostname) throws RemoteException {
        logRemoteMethodStartCall("getComputer");
        Integer result = target.getComputer(hostname);
        logRemoteMethodEndCall("getComputer", result);
        return result;
    }

    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreen");
        ExternalScreen result = target.getExternalScreen(screenID);
        logRemoteMethodEndCall("getExternalScreen", result);
        return result;        
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        logRemoteMethodStartCall("getExternalScreenParameters");
        ExternalScreenParameters result = target.getExternalScreenParameters(screenID, computerId);
        logRemoteMethodEndCall("getExternalScreenParameters", result);
        return result;
    }

    public void endSession(String clientInfo) throws RemoteException {
        target.endSession(clientInfo);
    }

    public boolean checkUser(String login, String password) throws RemoteException {
        return target.checkUser(login, password);
    }

    public byte[] getPropertyObjectsByteArray(byte[] classes, boolean isCompulsory, boolean isAny) throws RemoteException {
        return target.getPropertyObjectsByteArray(classes, isCompulsory, isAny);
    }

    public byte[] getBaseClassByteArray() throws RemoteException {
        return target.getBaseClassByteArray();
    }

    public int generateNewID()  throws RemoteException {
        return target.generateNewID();
    }

    @NonFlushRemoteMethod
    public void ping() throws RemoteException {
        target.ping();
    }

    @NonFlushRemoteMethod
    public byte[] findClass(String name) throws RemoteException {
        logRemoteMethodStartCall("findClass");
        byte[] result = target.findClass(name);
        logRemoteMethodEndCall("findClass", result);
        return result;
    }
}
