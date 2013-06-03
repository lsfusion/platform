package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.ChangeClassView;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.GClassViewType;
import lsfusion.interop.ClassViewType;

import java.io.IOException;

public class ChangeClassViewHandler extends ServerResponseActionHandler<ChangeClassView> {
    public ChangeClassViewHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeClassView action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(
                form,
                form.remoteForm.changeClassView(action.requestIndex, action.groupObjectId, convertClassView(action.newClassView))
        );
    }

    private ClassViewType convertClassView(GClassViewType newClassView) {
        return GwtToClientConverter.getInstance().convertOrNull(newClassView);
    }
}
