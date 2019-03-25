package lsfusion.http.provider;

import java.rmi.RemoteException;

// sometimes session can be invalidated (for example when logging out, spring immediately invalidates session, or on a mobile client when phone is locked)
// in that case it's pretty similar to nosuchobject exception, and we want this situation to be treated loke connection problems, thats why we extend remoteException
public class SessionInvalidatedException extends RemoteException {
}
