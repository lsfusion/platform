package lsfusion.client.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.interop.ClientSettings;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.SecuritySettings;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface> extends RemoteObjectProxy<T> implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public byte[] getCurrentUserInfoByteArray() throws RemoteException {
        logRemoteMethodStartCall("getCurrentUserInfoByteArray");
        byte[] result = target.getCurrentUserInfoByteArray();
        logRemoteMethodEndCall("getCurrentUserInfoByteArray", result);
        return result;
    }

    public void logClientException(String title, String hostname, Throwable t) throws RemoteException {
        target.logClientException(title, hostname, t);
    }

    public void close() throws RemoteException {
        logRemoteMethodStartCall("close");
        target.close();
        logRemoteMethodEndCall("close", "");
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    public void setUpdateTime(int updateTime) throws RemoteException {
        target.setUpdateTime(updateTime);
    }

    @Override
    public boolean isForbidDuplicateForms() throws RemoteException {
        logRemoteMethodStartCall("isForbidDuplicateForms");
        boolean result = target.isForbidDuplicateForms();
        logRemoteMethodEndCall("isForbidDuplicateForms", result);
        return result;
    }

    @Override
    public void setCurrentForm(String formID) throws RemoteException {
        target.setCurrentForm(formID);
    }

    @Override
    public String getCurrentForm() throws RemoteException {
        logRemoteMethodStartCall("getCurrentForm");
        String result = target.getCurrentForm();
        logRemoteMethodEndCall("getCurrentForm", result);
        return result;
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
    public SecuritySettings getSecuritySettings() throws RemoteException {
        return target.getSecuritySettings();
    }

    @Override
    public ClientSettings getClientSettings() throws RemoteException {
        return target.getClientSettings();
    }

    @Override
    public LocalePreferences getLocalePreferences() throws RemoteException {
        return target.getLocalePreferences();
    }

    @Override
    public Integer getFontSize() throws RemoteException {
        return target.getFontSize();
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID, int type) throws RemoteException {
        return target.executeNavigatorAction(navigatorActionSID, type);
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return target.continueNavigatorAction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException {
        return target.throwInNavigatorAction(clientThrowable);
    }
}
