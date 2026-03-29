package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.input.InputResult;

import java.util.function.Function;

public class PushExternalInput extends PushAsyncResult {
    public final Function<DataClass, InputResult> value;

    public PushExternalInput(Function<DataClass, InputResult> value) {
        this.value = value;
    }
}
