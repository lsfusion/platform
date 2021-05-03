package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.input.InputResult;

public class PushAsyncChange extends PushAsyncResult {
    public final InputResult value;

    public PushAsyncChange(ObjectValue value) {
        this(new InputResult(value, null));
    }

    public PushAsyncChange(InputResult value) {
        this.value = value;
    }
}
