package lsfusion.server.logics.navigator;

import lsfusion.server.base.AppImages;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;

public class NavigatorFolder extends NavigatorElement {
    public NavigatorFolder(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public String getDefaultIcon() {
        return isParentRoot() ? AppImages.OPENTOP : AppImages.OPEN;
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
    public AsyncExec getAsyncExec() {
        return null;
    }
}
