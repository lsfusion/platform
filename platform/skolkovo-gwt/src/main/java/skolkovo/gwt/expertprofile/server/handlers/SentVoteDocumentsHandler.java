package skolkovo.gwt.expertprofile.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.server.ExpertProfileDispatchServlet;
import skolkovo.gwt.expertprofile.shared.actions.SentVoteDocuments;

import java.io.IOException;

public class SentVoteDocumentsHandler extends SimpleActionHandlerEx<SentVoteDocuments, VoidResult, SkolkovoRemoteInterface> {
    public SentVoteDocumentsHandler(ExpertProfileDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SentVoteDocuments action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().sentVoteDocuments(ServerUtils.getAuthentication().getName(), action.voteId);
        return new VoidResult();
    }
}
