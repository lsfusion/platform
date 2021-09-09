package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.value.DataObject;

public class PushAsyncAdd extends PushAsyncResult {
    public final DataObject value;

    public PushAsyncAdd(DataObject value) {
        this.value = value;
    }
}
