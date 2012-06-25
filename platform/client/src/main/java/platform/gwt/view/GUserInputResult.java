package platform.gwt.view;

import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.Serializable;

public class GUserInputResult implements Serializable {
    public static final GUserInputResult canceled = new GUserInputResult(true);

    private final boolean editCanceled;
    private final ObjectDTO value;

    public GUserInputResult(boolean canceled) {
        this(canceled, null);
    }

    public GUserInputResult(Object value) {
        this(false, value);
    }

    public GUserInputResult(boolean canceled, Object value) {
        this.editCanceled = canceled;
        this.value = new ObjectDTO(value);
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public ObjectDTO getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", value=" + value + "]";
    }



}
