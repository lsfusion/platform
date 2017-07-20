package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ScrollToEnd;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.interop.Scroll;

import java.io.IOException;

public class ScrollToEndHandler extends ServerResponseActionHandler<ScrollToEnd> {
    public ScrollToEndHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ScrollToEnd action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Scroll scrollType = action.toEnd ? Scroll.END : Scroll.HOME;
        return getServerResponseResult(form,
                                       form.remoteForm.changeGroupObject(action.requestIndex, -1, action.groupId, scrollType.serialize()));
    }
}
