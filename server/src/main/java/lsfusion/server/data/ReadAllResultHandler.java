package lsfusion.server.data;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;

public class ReadAllResultHandler<K, V> implements ResultHandler<K, V> {

    private final MOrderExclMap<ImMap<K, Object>, ImMap<V, Object>> mExecResult = MapFact.mOrderExclMap();

    public void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) {
        mExecResult.exclAdd(rowKey, rowValue);
    }

    public void start() {
    }

    public void finish() {
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> terminate() {
        return mExecResult.immutableOrder(); 
    }

    public MOrderExclMap<ImMap<K, Object>, ImMap<V, Object>> getPrevResults() {
        return mExecResult;
    }
}
