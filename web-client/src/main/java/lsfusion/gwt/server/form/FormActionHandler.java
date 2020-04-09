package lsfusion.gwt.server.form;

import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
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

    protected FormProvider getFormProvider() {
        return servlet.getFormProvider();
    }
    public FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        return getFormProvider().getFormSessionObject(formSessionID);
    }

    @Override
    protected String getActionDetails(A action) throws SessionInvalidatedException {
        String message = super.getActionDetails(action);

        String formSessionID = action.formSessionID;
        FormSessionObject form = getFormSessionObject(formSessionID);
        if (form != null) {
            message += " : " + form.clientForm.canonicalName + "(" + formSessionID + ")";
        }
        if (action instanceof FormRequestAction) {
            message += " : " + ((FormRequestAction) action).requestIndex;
        }
        return message;
    }
}
