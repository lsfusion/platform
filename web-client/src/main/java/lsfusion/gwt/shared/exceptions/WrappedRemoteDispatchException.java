package lsfusion.gwt.shared.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

import java.rmi.RemoteException;

// exception wrapper to pass through ActionHandler.execute to MainDispatchServlet
// it contains RemoteException so should not be passed to client (should be catched and handled in servlet)
public class WrappedRemoteDispatchException extends DispatchException {
    
    public final RemoteException remoteException; 

    public WrappedRemoteDispatchException(RemoteException remoteException) {
        super(remoteException);
        
        this.remoteException = remoteException;
    }
}
