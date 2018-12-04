package lsfusion.gwt.form.server.navigator;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.CloseNavigator;
import lsfusion.gwt.form.shared.actions.navigator.NavigatorAction;
import lsfusion.gwt.form.shared.actions.logics.CreateNavigator;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class NavigatorActionHandler<A extends NavigatorAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    public FormProvider getFormSessionManager() {
        return ((LSFusionDispatchServlet) servlet).getFormProvider();
    }


    protected GForm createFormSessionObject() {

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
            message += "TAB ID " + ((CloseNavigator) action).tabSID + " IN " + servlet.getSessionInfo();
        }
        return message;
    }

}
