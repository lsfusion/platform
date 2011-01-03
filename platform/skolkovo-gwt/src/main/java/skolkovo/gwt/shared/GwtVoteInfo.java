package skolkovo.gwt.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtVoteInfo implements IsSerializable {
    //{{expert info
    public String expertName;
    //}}

    //{{project info
    public String projectClaimer;
    public String projectName;
    public String cluster;
    //}}

    //{{vote info
    public boolean interestedPerson;
    public boolean isConcurrentProject;
    public String concurrencyExplanation;
    public boolean isForeignSpecialistInvolved;
    public int competence;   //[1, 5]
    public int completeness; //[1, 5]
    public String completenessDescription;
    //}}

    public GwtVoteInfo() {
    }
}
