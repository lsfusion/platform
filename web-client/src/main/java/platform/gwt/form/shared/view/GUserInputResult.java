package platform.gwt.form.shared.view;

import java.io.Serializable;

public class GUserInputResult implements Serializable {
    public static final GUserInputResult canceled = new GUserInputResult(true);

    private boolean editCanceled;
    private Serializable value;

    @SuppressWarnings("UnusedDeclaration")
    public GUserInputResult() {}

    public GUserInputResult(boolean canceled) {
        this(canceled, null);
    }

    public GUserInputResult(Object value) {
        this(false, value);
    }

    public GUserInputResult(boolean canceled, Object value) {
        this.editCanceled = canceled;
        this.value = (Serializable) value;
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Serializable getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", value=" + value + "]";
    }



}
