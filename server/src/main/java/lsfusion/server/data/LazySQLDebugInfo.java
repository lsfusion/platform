package lsfusion.server.data;

import lsfusion.server.ServerLoggers;
import lsfusion.server.data.query.CompileOptions;
import lsfusion.server.data.query.CompiledQuery;
import lsfusion.server.data.query.IQuery;

import java.lang.ref.WeakReference;

// этот класс нужен в том числе чтобы debugInfo получать только на запросах превысивших threshold
public class LazySQLDebugInfo<K, V> {
    
    public LazySQLDebugInfo(IQuery<K, V> query, CompileOptions<V> compileOptions) {
        this.wQuery = new WeakReference<>(query);
        this.compileOptions = compileOptions;
    }

    private final WeakReference<IQuery<K, V>> wQuery;
    private final CompileOptions<V> compileOptions;
    
    private final static ThreadLocal<LazySQLDebugInfo> currentSQL = new ThreadLocal<>(); 
    public static LazySQLDebugInfo pushStack(LazySQLDebugInfo debugInfo) {
        if(debugInfo != null) {
            LazySQLDebugInfo result = currentSQL.get();
            currentSQL.set(debugInfo);
            return result;
        }
        return null;
    }
    
    public static <K, V> void outCompileDebugInfo() {
        LazySQLDebugInfo<K, V> debugInfo = currentSQL.get();
        if(debugInfo != null) {
            IQuery<K, V> query = debugInfo.wQuery.get();
            if(query != null) {
                ServerLoggers.assertLog(!debugInfo.compileOptions.needDebugInfo, "NO NEEDDEBUGINFO");
                CompiledQuery<K, V> compiledQuery = query.compile(debugInfo.compileOptions.debug());
                ServerLoggers.explainCompileLogger.info(compiledQuery.debugInfo); // assert что есть
            } else
                ServerLoggers.assertLog(!debugInfo.compileOptions.needDebugInfo, "QUERY SHOULD EXIST");
        }
    }
    public static void popStack(LazySQLDebugInfo debugInfo, LazySQLDebugInfo prevDebugInfo) {
        if(debugInfo != null)
            currentSQL.set(prevDebugInfo);
    }
}
