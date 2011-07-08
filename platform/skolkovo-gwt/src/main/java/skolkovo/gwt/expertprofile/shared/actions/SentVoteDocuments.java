package skolkovo.gwt.expertprofile.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;

public class SentVoteDocuments implements Action<VoidResult> {
    public int voteId;

    public SentVoteDocuments() {}

    public SentVoteDocuments(int voteId) {
        this.voteId = voteId;
    }
}
