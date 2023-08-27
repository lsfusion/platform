package lsfusion.server.logics.navigator;

import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;

public class NavigatorFolder extends NavigatorElement {
    public NavigatorFolder(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public String getDefaultIcon() {
        return isParentRoot() ? AppServerImage.OPENTOP : AppServerImage.OPEN;
    }

    @Override
    public boolean isLeafElement() {
        return false;
    }

    @Override
    public byte getTypeID() {
        return 1;
    }

    @Override
    public AsyncExec getAsyncExec(ConnectionContext context) {
        return null;
    }
}
