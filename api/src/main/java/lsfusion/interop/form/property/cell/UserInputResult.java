package lsfusion.interop.form.property.cell;

import java.io.Serializable;
import java.util.Arrays;

public class UserInputResult implements Serializable {
    public static final UserInputResult canceled = new UserInputResult(true, new Object[0], null);

    private final boolean editCanceled;
    private final Object[] values;
    private final Integer contextAction;

    public UserInputResult(Object[] values) {
        this(values, null);
    }

    public UserInputResult(Object[] values, Integer contextAction) {
        this(false, values, contextAction);
    }

    public UserInputResult(boolean canceled, Object[] values, Integer contextAction) {
        this.editCanceled = canceled;
        assert values != null;
        this.values = values;
        this.contextAction = contextAction;
    }

    public static UserInputResult singleValue(Object value, Integer contextAction) {
        return new UserInputResult(new Object[]{value}, contextAction);
    }

    public static UserInputResult singleValue(Object value) {
        return singleValue(value, null);
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Object[] getValues() {
        return values;
    }
    
    public Integer getContextAction() {
        return contextAction;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", values=" + Arrays.toString(values) + "]";
    }
}
