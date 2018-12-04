package lsfusion.gwt.form.server.form;

import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.shared.actions.form.FormAction;
import lsfusion.gwt.form.shared.actions.form.FormRequestIndexCountingAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class FormActionHandler<A extends FormAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {
    public FormActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    private FormProvider getFormSessionManager() {
        return ((LSFusionDispatchServlet)servlet).getFormProvider();
    }

    public FormSessionObject getFormSessionObject(String formSessionID) throws RuntimeException {
        return getFormSessionManager().getFormSessionObject(formSessionID);
    }

    public FormSessionObject getFormSessionObjectOrNull(String formSessionID) throws RuntimeException {
        return getFormSessionManager().getFormSessionObjectOrNull(formSessionID);
    }

    public FormSessionObject removeFormSessionObject(String formSessionID) throws RuntimeException {
        return getFormSessionManager().removeFormSessionObject(formSessionID);
    }

    protected final static int defaultLastReceivedRequestIndex = -1;

    @Override
    protected String getActionDetails(A action) {
        String message = super.getActionDetails(action);

        String formSessionID = action.formSessionID;
        FormSessionObject form = getFormSessionObjectOrNull(formSessionID);
        if (form != null) {
            message += " : " + form.clientForm.canonicalName + "(" + formSessionID + ")";
        }
        if (action instanceof FormRequestIndexCountingAction) {
            message += " : " + ((FormRequestIndexCountingAction) action).requestIndex;
        }
        return message;
    }
}
