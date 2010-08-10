package platform.client.remote.proxy;

import platform.interop.RemoteLogicsInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class RemoteBusinessLogicProxy<T extends RemoteLogicsInterface>
        extends RemoteObjectProxy<T>
        implements RemoteLogicsInterface {

    public RemoteBusinessLogicProxy(T target) {
        super(target);
    }

    public String getName() throws RemoteException {
        return target.getName();
    }

    public byte[] findClass(String name) throws RemoteException {
        return target.findClass(name);
    }

    public RemoteNavigatorInterface createNavigator(String login, String password, int computer) throws RemoteException {
        return new RemoteNavigatorProxy(target.createNavigator(login, password, computer));
    }

    public Integer getComputer(String hostname) throws RemoteException {
        return target.getComputer(hostname);
    }

    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        return target.getExternalScreen(screenID);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        return target.getExternalScreenParameters(screenID, computerId);
    }
}
