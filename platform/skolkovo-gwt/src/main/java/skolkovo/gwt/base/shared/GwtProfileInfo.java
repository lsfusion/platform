package skolkovo.gwt.base.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtProfileInfo implements IsSerializable {
    public String expertEmail;
    public String expertName;

    public GwtVoteInfo voteInfos[];

    public GwtProfileInfo() {
    }
}
