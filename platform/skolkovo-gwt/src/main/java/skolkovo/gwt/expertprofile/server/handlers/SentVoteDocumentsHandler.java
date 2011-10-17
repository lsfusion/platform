package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.gwt.expertprofile.server.ExpertProfileServiceImpl;
import skolkovo.gwt.expertprofile.shared.actions.SentVoteDocuments;

import java.io.IOException;

public class SentVoteDocumentsHandler extends SimpleActionHandlerEx<SentVoteDocuments, VoidResult> {
    protected final ExpertProfileServiceImpl servlet;

    public SentVoteDocumentsHandler(ExpertProfileServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public VoidResult executeEx(SentVoteDocuments action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().sentVoteDocuments(ServerUtils.getAuthentication().getName(), action.voteId);
        return new VoidResult();
    }
}
