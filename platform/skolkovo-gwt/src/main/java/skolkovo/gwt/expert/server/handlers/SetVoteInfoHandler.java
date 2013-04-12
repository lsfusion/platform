package skolkovo.gwt.expert.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expert.server.ExpertDispatchServlet;
import skolkovo.gwt.expert.shared.actions.SetVoteInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SetVoteInfoHandler extends SimpleActionHandlerEx<SetVoteInfo, VoidResult, SkolkovoRemoteInterface> {
    public SetVoteInfoHandler(ExpertDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SetVoteInfo action, ExecutionContext context) throws DispatchException, IOException {
        HttpServletRequest request = servlet.getRequest();
        String ipAddress  = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        action.voteInfo.expertIP = ipAddress;
        servlet.getLogics().setVoteInfo(action.voteId, action.voteInfo);
        return new VoidResult();
    }
}
