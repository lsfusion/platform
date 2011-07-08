package skolkovo.gwt.expert.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import skolkovo.gwt.expert.server.ExpertServiceImpl;
import skolkovo.gwt.expert.shared.actions.GetVoteInfo;
import skolkovo.gwt.expert.shared.actions.GetVoteInfoResult;

import java.io.IOException;

public class GetVoteInfoHandler extends SimpleActionHandlerEx<GetVoteInfo, GetVoteInfoResult> {
    private final ExpertServiceImpl servlet;

    public GetVoteInfoHandler(ExpertServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public GetVoteInfoResult executeEx(GetVoteInfo action, ExecutionContext context) throws DispatchException, IOException {
        return new GetVoteInfoResult(servlet.getLogics().getVoteInfo(action.voteId));
    }
}
