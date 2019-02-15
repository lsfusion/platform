package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.shared.actions.form.ScrollToEnd;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.interop.Scroll;

import java.io.IOException;

public class ScrollToEndHandler extends FormServerResponseActionHandler<ScrollToEnd> {
    public ScrollToEndHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ScrollToEnd action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Scroll scrollType = action.toEnd ? Scroll.END : Scroll.HOME;
        return getServerResponseResult(form,
                                       form.remoteForm.changeGroupObject(action.requestIndex, defaultLastReceivedRequestIndex, action.groupId, scrollType.serialize()));
    }
}
