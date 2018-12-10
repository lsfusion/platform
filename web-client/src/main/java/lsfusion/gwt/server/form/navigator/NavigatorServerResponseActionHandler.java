package lsfusion.gwt.server.form.navigator;

import lsfusion.gwt.server.form.form.spring.FormSessionObject;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.form.actions.navigator.NavigatorRequestAction;
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
