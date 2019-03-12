package lsfusion.server.data;

import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.query.CompileOptions;
import lsfusion.server.data.query.CompiledQuery;
import lsfusion.server.data.query.IQuery;

import java.lang.ref.WeakReference;

// этот класс нужен в том числе чтобы debugInfo получать только на запросах превысивших threshold
public class SQLDebugInfo<K, V> {

    public SQLDebugInfo(IQuery<K, V> query, CompileOptions<V> compileOptions) {
        this.wQuery = new WeakReference<>(query);
        this.compileOptions = compileOptions;
    }

    private final WeakReference<IQuery<K, V>> wQuery;
    private final CompileOptions<V> compileOptions;
    private boolean alreadyExplained; // для материализации подзапросов чтобы несколько раз не выводить 

    private static ConcurrentWeakHashMap<Thread, SQLDebugInfo> sqlDebugInfoMap = MapFact.getGlobalConcurrentWeakHashMap();

    public static SQLDebugInfo pushStack(SQLDebugInfo debugInfo) {
        if (debugInfo != null) {
            return sqlDebugInfoMap.put(Thread.currentThread(), debugInfo);
        }
        return null;
    }

    public static <K, V> void outCompileDebugInfo(String statementInfo) {
        SQLDebugInfo<K, V> debugInfo = sqlDebugInfoMap.get(Thread.currentThread());
        if (debugInfo != null) {
            IQuery<K, V> query = debugInfo.wQuery.get();
            if (query != null) {
                ServerLoggers.assertLog(!debugInfo.compileOptions.needDebugInfo, "NO NEEDDEBUGINFO");
                if (!debugInfo.alreadyExplained) {
                    CompiledQuery<K, V> compiledQuery = query.compile(debugInfo.compileOptions.debug());
                    ServerLoggers.explainCompileLogger.info(statementInfo);
//                    for(String line : compiledQuery.debugInfo.split("\n")) // assert что есть
//                        ServerLoggers.explainCompileLogger.info(line);
                    ServerLoggers.explainCompileLogger.info(compiledQuery.debugInfo); // очень большой лог в info может очень долго выводить
                    debugInfo.alreadyExplained = true;
                }
            } else
                ServerLoggers.assertLog(!debugInfo.compileOptions.needDebugInfo, "QUERY SHOULD EXIST");
        }
    }

    public static void popStack(SQLDebugInfo debugInfo, SQLDebugInfo prevDebugInfo) {
        if (debugInfo != null) {
            if (prevDebugInfo != null)
                sqlDebugInfoMap.put(Thread.currentThread(), prevDebugInfo);
            else
                sqlDebugInfoMap.remove(Thread.currentThread());
        }
    }

    public static ConcurrentWeakHashMap<Thread, SQLDebugInfo> getSqlDebugInfoMap() {
        return sqlDebugInfoMap;
    }

    public String getDebugInfoForProcessMonitor() {
        IQuery iQuery = wQuery.get();
        StringBuilder debugInfo = new StringBuilder();
        if (iQuery != null) {
            ImSet<Value> contextValues = iQuery.getContextValues();
            for (Value contextValue : contextValues) {
                debugInfo.append(debugInfo.length() == 0 ? "" : "\n").append(contextValue.toDebugString());
            }
        }
        return debugInfo.length() == 0 ? null : debugInfo.toString();
    }
}
