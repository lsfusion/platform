package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;

public class GUserInputResult implements Serializable {
    public static final GUserInputResult canceled = new GUserInputResult(true, null, null);

    private boolean editCanceled;
    private Serializable value;
    private Integer contextAction;

    @SuppressWarnings("UnusedDeclaration")
    public GUserInputResult() {}

//    paste / custom
    public GUserInputResult(PValue value) {
        this(value, null);
    }

    // editor + context (value = null) + paste / custom
    public GUserInputResult(PValue value, Integer contextAction) {
        this(false, value, contextAction);
    }
    public GUserInputResult(boolean canceled, PValue value, Integer contextAction) {
        this.editCanceled = canceled;
        this.value = PValue.convertFileValueBack(value);
        this.contextAction = contextAction;

        this.pValue = value;
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Serializable getValue() {
        return value;
    }

    private transient PValue pValue;
    public PValue getPValue() {
        return pValue;
    }

    public Integer getContextAction() {
        return contextAction;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", value=" + value + "]";
    }



}
