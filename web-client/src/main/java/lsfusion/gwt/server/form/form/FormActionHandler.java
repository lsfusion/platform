package lsfusion.gwt.server.form.form;

import lsfusion.gwt.server.form.form.spring.FormSessionObject;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.spring.FormProvider;
import lsfusion.gwt.shared.form.actions.form.FormAction;
import lsfusion.gwt.shared.form.actions.form.FormRequestIndexCountingAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class FormActionHandler<A extends FormAction<R>, R extends Result> extends lsfusion.gwt.server.base.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {
    public FormActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    private FormProvider getFormProvider() {
        return servlet.getFormProvider();
    }
    public FormSessionObject getFormSessionObject(String formSessionID) throws RuntimeException {
        return getFormProvider().getFormSessionObject(formSessionID);
    }
    public void removeFormSessionObject(String formSessionID) throws RuntimeException {
        getFormProvider().removeFormSessionObject(formSessionID);
    }

    protected final static int defaultLastReceivedRequestIndex = -1;

    @Override
    protected String getActionDetails(A action) {
        String message = super.getActionDetails(action);

        String formSessionID = action.formSessionID;
        FormSessionObject form = getFormSessionObject(formSessionID);
        if (form != null) {
            message += " : " + form.clientForm.canonicalName + "(" + formSessionID + ")";
        }
        if (action instanceof FormRequestIndexCountingAction) {
            message += " : " + ((FormRequestIndexCountingAction) action).requestIndex;
        }
        return message;
    }
}
