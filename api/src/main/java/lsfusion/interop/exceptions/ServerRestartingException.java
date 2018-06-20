package lsfusion.interop.exceptions;

import lsfusion.base.ApiResourceBundle;

public class ServerRestartingException extends RemoteMessageException {

    public ServerRestartingException() {
        super(ApiResourceBundle.getString("exceptions.server.is.restarting"));
    }
}
