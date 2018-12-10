package lsfusion.gwt.form.server.navigator;

import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.NavigatorRequestAction;
import lsfusion.interop.form.ServerResponse;

import java.io.IOException;

public abstract class NavigatorServerResponseActionHandler<A extends NavigatorRequestAction> extends NavigatorActionHandler<A, ServerResponseResult> {

    protected NavigatorServerResponseActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(A action, ServerResponse serverResponse) throws IOException {
        return FormServerResponseActionHandler.getServerResponseResult(new FormSessionObject(null, null, action.sessionID), serverResponse, servlet);
    }
}
