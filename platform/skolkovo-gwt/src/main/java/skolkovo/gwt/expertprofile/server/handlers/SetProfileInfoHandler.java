package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.gwt.expertprofile.server.ExpertProfileServiceImpl;
import skolkovo.gwt.expertprofile.shared.actions.SetProfileInfo;

import java.io.IOException;

public class SetProfileInfoHandler extends SimpleActionHandlerEx<SetProfileInfo, VoidResult> {
    private final ExpertProfileServiceImpl servlet;

    public SetProfileInfoHandler(ExpertProfileServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public VoidResult executeEx(SetProfileInfo action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().setProfileInfo(action.expertLogin, action.profileInfo);
        return new VoidResult();
    }
}
