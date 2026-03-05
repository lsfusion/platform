package lsfusion.server.logics.navigator.controller.env;

import lsfusion.interop.action.ProcessNavigatorChangesClientAction;

public interface NavigatorRefreshController {
    void refresh();
    ProcessNavigatorChangesClientAction getNavigatorChangesAction();
}