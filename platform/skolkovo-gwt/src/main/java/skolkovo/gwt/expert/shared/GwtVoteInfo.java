package skolkovo.gwt.expert.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class GwtVoteInfo implements IsSerializable {
    //{{expert info
    public String expertName = "";
    //}}

    //{{project info
    public String projectClaimer = "";
    public String projectName = "";
    public String projectCluster = "";
    //}}

    //{{vote info
    public boolean voteDone;
    public String voteResult = "";
    public boolean inCluster;
    public boolean innovative;
    public String innovativeComment = "";
    public boolean foreign;
    public int competent = 1;   //[1, 5]
    public int complete = 1; //[1, 5]
    public String completeComment = "";
    //}}

    public Date date;

    public GwtVoteInfo() {
    }
}
