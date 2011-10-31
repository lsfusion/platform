package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import skolkovo.gwt.expertprofile.server.ExpertProfileServiceImpl;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfo;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfoResult;

import java.io.IOException;

public class GetProfileInfoHandler extends SimpleActionHandlerEx<GetProfileInfo, GetProfileInfoResult> {
    private final ExpertProfileServiceImpl servlet;

    public GetProfileInfoHandler(ExpertProfileServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public GetProfileInfoResult executeEx(GetProfileInfo action, ExecutionContext context) throws DispatchException, IOException {
        return new GetProfileInfoResult(servlet.getLogics().getProfileInfo(ServerUtils.getAuthentication().getName(), action.locale));
    }
}
