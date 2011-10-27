package skolkovo.api.gwt.shared;

import java.io.Serializable;

public class ProfileInfo implements Serializable {
    public String expertEmail;
    public String expertName;

    public boolean technical;
    public boolean business;

    public VoteInfo voteInfos[];
    public ForesightInfo foresightInfos[];
}
