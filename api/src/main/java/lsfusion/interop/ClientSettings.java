package lsfusion.interop;

import java.io.Serializable;

public class ClientSettings implements Serializable {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean useRequestTimeout;

    public ClientSettings(boolean busyDialog, long busyDialogTimeout, boolean useRequestTimeout) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
    }
}