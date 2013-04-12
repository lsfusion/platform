package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.server.ExpertProfileDispatchServlet;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfo;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfoResult;

import java.io.IOException;

public class GetProfileInfoHandler extends SimpleActionHandlerEx<GetProfileInfo, GetProfileInfoResult, SkolkovoRemoteInterface> {
    public GetProfileInfoHandler(ExpertProfileDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetProfileInfoResult executeEx(GetProfileInfo action, ExecutionContext context) throws DispatchException, IOException {
        return new GetProfileInfoResult(servlet.getLogics().getProfileInfo(ServerUtils.getAuthentication().getName(), ServerUtils.getLocaleLanguage()));
    }
}
