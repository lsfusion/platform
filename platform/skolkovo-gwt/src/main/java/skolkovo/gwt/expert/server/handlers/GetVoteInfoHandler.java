package skolkovo.gwt.expert.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expert.server.ExpertDispatchServlet;
import skolkovo.gwt.expert.shared.actions.GetVoteInfo;
import skolkovo.gwt.expert.shared.actions.GetVoteInfoResult;

import java.io.IOException;

public class GetVoteInfoHandler extends SimpleActionHandlerEx<GetVoteInfo, GetVoteInfoResult, SkolkovoRemoteInterface> {
    public GetVoteInfoHandler(ExpertDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetVoteInfoResult executeEx(GetVoteInfo action, ExecutionContext context) throws DispatchException, IOException {
        return new GetVoteInfoResult(servlet.getLogics().getVoteInfo(action.voteId, ServerUtils.getLocaleLanguage()));
    }
}
