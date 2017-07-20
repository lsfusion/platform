package lsfusion.gwt.form.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.GUserInputResult;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;

import java.sql.Date;
import java.sql.Timestamp;

public class GetFormResult implements Result {
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
    private ColorDTO exposeColorDTO;

    public GForm form;

    @SuppressWarnings("UnusedDeclaration")
    public GetFormResult() {}

    public GetFormResult(GForm form) {
        this.form = form;
    }
}