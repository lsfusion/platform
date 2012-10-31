package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form2.server.FormSessionObject;
import platform.gwt.form2.shared.view.GClassViewType;
import platform.gwt.form2.server.convert.GwtToClientConverter;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ChangeClassView;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.interop.ClassViewType;

import java.io.IOException;

public class ChangeClassViewHandler extends ServerResponseActionHandler<ChangeClassView> {
    public ChangeClassViewHandler(RemoteServiceImpl servlet) {
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
