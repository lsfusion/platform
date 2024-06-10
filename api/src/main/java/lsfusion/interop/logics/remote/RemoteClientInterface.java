package lsfusion.interop.logics.remote;

import lsfusion.interop.session.SessionInfo;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClientInterface extends Remote {

    String[] convertFileValue(SessionInfo sessionInfo, Serializable[] files) throws RemoteException;
}
