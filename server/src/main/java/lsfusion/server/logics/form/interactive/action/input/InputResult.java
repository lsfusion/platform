package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.interop.form.property.cell.UserInputResult;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.data.DataClass;

public class InputResult {
    
    public final ObjectValue value;
    public final Integer contextAction;

    public InputResult(ObjectValue value, Integer contextAction) {
        this.value = value;
        this.contextAction = contextAction;
    }

    public static InputResult get(UserInputResult result, DataClass dataClass) {
        assert !result.isCanceled();
        return new InputResult(ObjectValue.getValue(result.getValue(), dataClass), result.getContextAction());
    }
}
