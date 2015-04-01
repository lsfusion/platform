package lsfusion.server.data;

import lsfusion.base.Provider;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;

import java.sql.SQLException;

public interface ResultHandler<K, V>  {
    void start();
    
    void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) throws SQLException;
    
    void finish() throws SQLException;
    
    Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> getPrevResults();
}
