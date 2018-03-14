package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.base.ProgressBar;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessage;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessageList;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetRemoteNavigatorActionMessageListHandler extends LoggableActionHandler<GetRemoteNavigatorActionMessageList, ListResult, RemoteLogicsInterface> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    public GetRemoteNavigatorActionMessageListHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public ListResult executeEx(GetRemoteNavigatorActionMessageList action, ExecutionContext context) throws DispatchException, IOException {
        List<Object> result = new ArrayList<>();
        for (Object object : servlet.getNavigator().getRemoteActionMessageList()) {
            if (object instanceof ProgressBar)
                result.add(clientActionConverter.convertProgressBar((lsfusion.base.ProgressBar) object));
            else
                result.add(object);
        }
        return new ListResult((ArrayList) result);
    }
}