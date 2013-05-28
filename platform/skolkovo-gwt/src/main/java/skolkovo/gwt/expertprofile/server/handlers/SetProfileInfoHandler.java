package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.server.ExpertProfileDispatchServlet;
import skolkovo.gwt.expertprofile.shared.actions.SetProfileInfo;

import java.io.IOException;

public class SetProfileInfoHandler extends SimpleActionHandlerEx<SetProfileInfo, VoidResult, SkolkovoRemoteInterface> {
    public SetProfileInfoHandler(ExpertProfileDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SetProfileInfo action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().setProfileInfo(ServerUtils.getAuthentication().getName(), action.profileInfo);
        return new VoidResult();
    }
}
