package lsfusion.server.data.query.result;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.lambda.Provider;

public class ReadDistinctValuesHandler<K, V> implements ResultHandler<K, V>, Provider<ImOrderSet<ImMap<V, Object>>> {

    private final MOrderExclSet<ImMap<V, Object>> mExecResult = SetFact.mOrderExclSet();

    public void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) {
        mExecResult.exclAdd(rowValue);
    }

    public void start() {
    }

    public void finish() {
    }

    public ImOrderSet<ImMap<V, Object>> terminate() {
        return mExecResult.immutableOrder();
    }

    public ImOrderSet<ImMap<V, Object>> get() {
        return mExecResult.immutableOrderCopy();
    }

    public Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> getPrevResults() {
        throw new UnsupportedOperationException();
    }

    public boolean hasQueryLimit() {
        return false;
    }
}
