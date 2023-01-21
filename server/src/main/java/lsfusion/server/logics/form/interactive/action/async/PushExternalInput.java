package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;

import java.util.function.Function;

public class PushExternalInput extends PushAsyncResult {
    public final Function<Type, Object> value;

    public PushExternalInput(Function<Type, Object> value) {
        this.value = value;
    }
}
