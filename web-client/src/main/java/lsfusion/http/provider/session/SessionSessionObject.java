package lsfusion.http.provider.session;

import lsfusion.interop.session.remote.RemoteSessionInterface;

public class SessionSessionObject {

    public final RemoteSessionInterface remoteSession;

    public SessionSessionObject(RemoteSessionInterface remoteSession) {
        this.remoteSession = remoteSession;
    }
}
