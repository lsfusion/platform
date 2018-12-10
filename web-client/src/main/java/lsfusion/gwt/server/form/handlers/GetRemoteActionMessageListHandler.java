package lsfusion.gwt.server.form.handlers;

import lsfusion.base.ProgressBar;
import lsfusion.gwt.shared.actions.ListResult;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.gwt.shared.form.actions.form.GetRemoteActionMessageList;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetRemoteActionMessageListHandler extends FormActionHandler<GetRemoteActionMessageList, ListResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    public GetRemoteActionMessageListHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteActionMessageList action) {
        return null; // too many logs
    }

    @Override
    public ListResult executeEx(GetRemoteActionMessageList action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        List<Object> result = new ArrayList<>();
        if (form != null) {

            for (Object object : form.remoteForm.getRemoteActionMessageList()) {
                if (object instanceof ProgressBar)
                    result.add(clientActionConverter.convertProgressBar((lsfusion.base.ProgressBar) object));
                else
                    result.add(object);
            }
        }
        return new ListResult((ArrayList) result);
    }
}