package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.input.InputResult;

public class PushAsyncInput extends PushAsyncResult {
    public final InputResult value;

    public PushAsyncInput(ObjectValue value) {
        this(new InputResult(value, null));
    }

    public PushAsyncInput(InputResult value) {
        this.value = value;
    }
}
