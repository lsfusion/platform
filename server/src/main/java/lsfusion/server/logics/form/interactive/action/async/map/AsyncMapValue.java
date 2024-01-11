package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class AsyncMapValue<T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final DataClass type;

    public AsyncMapValue(DataClass type) {
        this.type = type;
    }


    @Override
    public int getOptimisticPriority() {
        return 2;
    }

    @Override
    public boolean needOwnPushResult() {
        return true; // we have to send input value
    }

    public abstract <X extends PropertyInterface> Pair<InputListEntity<X, T>, AsyncDataConverter<X>> getAsyncValueList(Result<String> value);
}
