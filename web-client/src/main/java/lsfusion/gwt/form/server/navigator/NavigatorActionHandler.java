package lsfusion.gwt.form.server.navigator;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.CloseNavigator;
import lsfusion.gwt.form.shared.actions.navigator.NavigatorAction;
import lsfusion.gwt.form.shared.actions.logics.CreateNavigator;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class NavigatorActionHandler<A extends NavigatorAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    private NavigatorProvider getNavigatorProvider() {
        return ((LSFusionDispatchServlet)servlet).getNavigatorProvider();
    }
    protected NavigatorSessionObject getNavigatorSessionObject(String navigatorID) {
        return getNavigatorProvider().getNavigatorSessionObject(navigatorID);
    }
    protected void removeNavigatorSessionObject(String navigatorID) {
        getNavigatorProvider().removeNavigatorSessionObject(navigatorID);

        нужно убрать еще в formProvider'е все
        если не осталось навигаторов можно закрывать
        formProvider.removeFormSessionObjects(tabSID);

        if (navigatorProvider.tabClosed(tabSID)) {
            invalidate();
        }

    }

    // shortcut's
    protected NavigatorSessionObject getNavigatorSessionObject(A action) {
        return getNavigatorSessionObject(action.navigatorID);
    }
    protected RemoteNavigatorInterface getRemoteNavigator(A action) {
        return getNavigatorSessionObject(action).remoteNavigator;
    }

    public NavigatorActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    protected String getActionDetails(A action) {
        String message = super.getActionDetails(action);

        if (action instanceof CreateNavigator) {
            message += "TAB ID " + ((CreateNavigator) action).tabSID + " IN " + servlet.getSessionInfo();
        }
        if (action instanceof CloseNavigator) {
            message += "TAB ID " + ((CloseNavigator) action).navigatorID + " IN " + servlet.getSessionInfo();
        }
        return message;
    }

}
