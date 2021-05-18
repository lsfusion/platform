package lsfusion.gwt.client.form.property.cell.view;

import java.io.Serializable;

public class GUserInputResult implements Serializable {
    public static final GUserInputResult canceled = new GUserInputResult(true, null, null);

    private boolean editCanceled;
    private Serializable value;
    private Integer contextAction;

    @SuppressWarnings("UnusedDeclaration")
    public GUserInputResult() {}

    public GUserInputResult(Object value) {
        this(value, null);
    }

    public GUserInputResult(Object value, Integer contextAction) {
        this(false, value, contextAction);
    }

    public GUserInputResult(boolean canceled, Object value, Integer contextAction) {
        this.editCanceled = canceled;
        this.value = (Serializable) value;
        this.contextAction = contextAction;
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Serializable getValue() {
        return value;
    }

    public Integer getContextAction() {
        return contextAction;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", value=" + value + "]";
    }



}
