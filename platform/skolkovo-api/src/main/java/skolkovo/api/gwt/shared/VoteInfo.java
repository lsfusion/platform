package skolkovo.api.gwt.shared;

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
    public String revision;
    //}}

    //{{vote R1 info
    public boolean inCluster;
    public boolean innovative;
    public String innovativeComment;
    public boolean foreign;
    public int competent;   //[1, 5]
    public int complete; //[1, 5]
    public String completeComment;
    //}}

    //{{vote R2 info
    public boolean competitiveAdvantages;
    public String competitiveAdvantagesComment;
    public boolean commercePotential;
    public String commercePotentialComment;
    public boolean implement;
    public String implementComment;
    public boolean expertise;
    public String expertiseComment;
    public boolean internationalExperience;
    public String internationalExperienceComment;
    public boolean enoughDocuments;
    public String enoughDocumentsComment;
    //}}

    public Date date;
    public Date voteStartDate;
    public Date voteEndDate;
    public String expertIP;

    public boolean isVoted() {
        return "voted".equals(voteResult);
    }
}
