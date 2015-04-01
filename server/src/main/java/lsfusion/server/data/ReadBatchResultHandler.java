package lsfusion.server.data;

import lsfusion.base.Provider;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;

import java.sql.SQLException;

public abstract class ReadBatchResultHandler<K, V> implements ResultHandler<K, V>, Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> {

    private final int batchThreshold;

    protected ReadBatchResultHandler(int batchThreshold) {
        this.batchThreshold = batchThreshold;
    }

    private MOrderExclMap<ImMap<K, Object>, ImMap<V, Object>> mExecResult = MapFact.mOrderExclMap();
    
    public abstract void proceedBatch(ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> batch) throws SQLException;

    public void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) throws SQLException {
        mExecResult.exclAdd(rowKey, rowValue);
        if(mExecResult.size() >= batchThreshold) {
            proceedBatch();
            mExecResult = MapFact.mOrderExclMap();
        }
    }
    public void proceedBatch() throws SQLException {
        proceedBatch(mExecResult.immutableOrder());
    }    
    
    public void finish() throws SQLException {
        proceedBatch();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> get() {
        return mExecResult.immutableOrderCopy();
    }

    public Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> getPrevResults() {
        return this;
    }
}
