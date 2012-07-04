package platform.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.view.GUserInputResult;
import platform.gwt.view.actions.GAction;

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
