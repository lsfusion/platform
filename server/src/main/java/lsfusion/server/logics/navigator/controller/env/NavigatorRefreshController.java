package lsfusion.server.logics.navigator.controller.env;

import lsfusion.interop.action.ClientAction;

public interface NavigatorRefreshController {
    void refresh();
    ClientAction getNavigatorChangesAction();
}