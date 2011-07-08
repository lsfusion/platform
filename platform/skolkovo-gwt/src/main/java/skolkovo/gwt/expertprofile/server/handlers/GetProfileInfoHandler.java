package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import org.springframework.security.core.Authentication;
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
        Authentication auth = servlet.getAuthentication();
        if (auth == null) {
            return null;
        }

        return new GetProfileInfoResult(servlet.getLogics().getProfileInfo(auth.getName()));
    }
}
