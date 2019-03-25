package lsfusion.gwt.server.form;

import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestIndexCountingAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.SimpleActionHandlerEx;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.shared.Result;

public abstract class FormActionHandler<A extends FormAction<R>, R extends Result> extends SimpleActionHandlerEx<A, R> {
    public FormActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    private FormProvider getFormProvider() {
        return servlet.getFormProvider();
    }
    public FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        return getFormProvider().getFormSessionObject(formSessionID);
    }
    public void removeFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        getFormProvider().removeFormSessionObject(formSessionID);
    }

    public final static int defaultLastReceivedRequestIndex = -2;

    @Override
    protected String getActionDetails(A action) throws SessionInvalidatedException {
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
