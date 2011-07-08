package skolkovo.gwt.expert.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.gwt.expert.server.ExpertServiceImpl;
import skolkovo.gwt.expert.shared.actions.SetVoteInfo;

import java.io.IOException;

public class SetVoteInfoHandler extends SimpleActionHandlerEx<SetVoteInfo, VoidResult> {
    protected final ExpertServiceImpl servlet;

    public SetVoteInfoHandler(ExpertServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public VoidResult executeEx(SetVoteInfo action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().setVoteInfo(action.voteId, action.voteInfo);
        return new VoidResult();
    }
}
