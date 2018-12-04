package lsfusion.gwt.form.server.logics;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.form.FormAction;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class LogicsActionHandler<A extends LogicsAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    public LogicsActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    public NavigatorProvider createNavigatorProvider() {
        return ((LSFusionDispatchServlet)servlet).getFormProvider();
    }

}
