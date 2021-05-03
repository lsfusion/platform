package lsfusion.interop.form.property.cell;

import java.io.Serializable;

public class UserInputResult implements Serializable {
    public static final UserInputResult canceled = new UserInputResult(true, null, null);

    private final boolean editCanceled;
    
    private final Object value;
    private final Integer contextAction;

    public UserInputResult(Object value) {
        this(value, null);
    }

    public UserInputResult(Object value, Integer contextAction) {
        this(false, value, contextAction);
    }

    public UserInputResult(boolean canceled, Object value, Integer contextAction) {
        this.editCanceled = canceled;
        this.value = value;
        this.contextAction = contextAction;
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Object getValue() {
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
