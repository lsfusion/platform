package platform.gwt.form2.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.view2.GUserInputResult;
import platform.gwt.view2.actions.GAction;

import java.sql.Date;
import java.sql.Timestamp;

public class ServerResponseResult implements Result {
    //нужно, чтобы об этих типах знал механизм сериализации GWT
    private Integer exposeInteger;
    private Long exposeLong;
    private Double exposeDouble;
    private Boolean exposeBoolean;
    private String exposeString;
    private Date exposeDate;
    private Timestamp exposeTimestamp;
    private byte[] exposeByteArray;
    private GUserInputResult exposeInputResult;

    public GAction[] actions;
    public boolean resumeInvocation;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}
