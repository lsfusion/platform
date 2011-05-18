package skolkovo;

import java.io.Serializable;
import java.util.Date;

public class VoteInfo implements Serializable {
    //{{expert info
    public int voteId;
    public String linkHash;
    public String expertName;
    //}}

    //{{project info
    public String projectClaimer;
    public String projectName;
    public String projectCluster;
    //}}

    //{{vote info
    public boolean voteDone;
    public String voteResult;
    public boolean inCluster;
    public boolean innovative;
    public String innovativeComment;
    public boolean foreign;
    public int competent;   //[1, 5]
    public int complete; //[1, 5]
    public String completeComment;
    //}}

    public Date date;
    public Date voteStartDate;
    public Date voteEndDate;
}
