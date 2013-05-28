package platform.interop.form;

import java.io.Serializable;

public class UserInputResult implements Serializable {
    public static final UserInputResult canceled = new UserInputResult(true);

    private final boolean editCanceled;
    private final Object value;

    public UserInputResult(boolean canceled) {
        this(canceled, null);
    }

    public UserInputResult(Object value) {
        this(false, value);
    }

    public UserInputResult(boolean canceled, Object value) {
        this.editCanceled = canceled;
        this.value = value;
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", value=" + value + "]";
    }
}
