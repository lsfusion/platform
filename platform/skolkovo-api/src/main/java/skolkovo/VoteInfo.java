package skolkovo;

import java.io.Serializable;

public class VoteInfo implements Serializable {
    //{{expert info
    public String expertName;
    //}}

    //{{project info
    public String projectClaimer;
    public String projectName;
    public String projectCluster;
    //}}

    //{{vote info
    public boolean connected;
    public boolean inCluster;
    public boolean innovative;
    public String innovativeComment;
    public boolean foreign;
    public int competent;   //[1, 5]
    public int complete; //[1, 5]
    public String completeComment;
    //}}
}
