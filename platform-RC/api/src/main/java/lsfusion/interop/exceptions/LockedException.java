package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class LockedException extends RemoteMessageException {
    public LockedException() {
        super(getString("exceptions.user.locked"));
    }
}
