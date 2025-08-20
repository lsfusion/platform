package lsfusion.server.data.sql.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.AssertSynchronized;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.FillTemporaryTable;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

// выделяется в отдельный объект так как синхронизироваться должен
public class SQLTemporaryPool {
    private final Map<FieldStruct, Set<String>> tables = MapFact.mAddRemoveMap();
    private final Map<String, Object> stats = MapFact.mAddRemoveMap();
    private final Map<String, FieldStruct> structs = MapFact.mAddRemoveMap(); // чтобы удалять таблицы, не имея структры
    private int counter = 0;

    public SQLTemporaryPool() {
        log = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(Settings.get().getExplainTemporaryTablesLogSize()));
        allLogs.add(log);
    }
    public final Buffer log;
    private static final ConcurrentIdentityWeakHashSet<Buffer> allLogs = new ConcurrentIdentityWeakHashSet<>();
    public static void removeAllLogs() {
        for(Buffer log : allLogs)
            log.clear();
    }
    public void addLog(String log, String table, OperationOwner owner) {
        this.log.add(new Pair<>(table, SQLSession.getCurrentTimeStamp() + " " + log + " " + owner + " " + ExecutionStackAspect.getExStackTrace()));
    }
    public void outLog(String table) {
        ServerLoggers.sqlHandLogger.info("TABLE DUMP: " + table);
        for (Object ff : log) {
            Pair<String, String> pff = (Pair<String, String>) ff;
            if(pff.first == null || pff.first.equals(table))
                ServerLoggers.sqlHandLogger.info(pff.second);
        }
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean isEmpty() {
        return tables.isEmpty();
    }
    
    public void checkAliveTables(SQLSession session, Map<String, WeakReference<TableOwner>> used) {
        try {
            System.out.println("START " + SQLSession.getCurrentTimeStamp() + " " + session);
            for(Map.Entry<FieldStruct, Set<String>> table : tables.entrySet())
                for(String tab : table.getValue()) {
//                    if(!used.containsKey(tab)) {
                        System.out.println("CHECK "  + SQLSession.getCurrentTimeStamp() + " " + tab + " " + session);
                        session.debugExecute("INSERT INTO " + session.syntax.getSessionTableName(tab) + " SELECT * FROM " + session.syntax.getSessionTableName(tab) + " WHERE 1 > 2");
                    }
            System.out.println("FINISHED " + SQLSession.getCurrentTimeStamp() + " " + session);
        } catch (SQLException e) {
            e = e;
        }
    }

    // SQLSession.assertLock : temporaryTables.lock + read
    @AssertSynchronized
    public String getTable(SQLSession session, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, Map<String, WeakReference<TableOwner>> used, Map<String, String> debugInfo, Result<Boolean> isNew, TableOwner owner, OperationOwner opOwner) throws SQLException { //, Map<String, String> usedStacks
        FieldStruct fieldStruct = new FieldStruct(keys, properties, count);

        Set<String> matchTables = tables.get(fieldStruct);
        if(matchTables==null) {
            matchTables = SetFact.mAddRemoveSet();
            tables.put(fieldStruct, matchTables);
        }

        for(String matchTable : matchTables) // ищем нужную таблицу
            if(!used.containsKey(matchTable)) { // если не используется
                assert !used.containsKey(matchTable);
                used.put(matchTable, new WeakReference<>(owner));
                debugInfo.put(matchTable, owner.getDebugInfo());
//                session.truncate(matchTable); // удаляем старые данные
//                if(session.getCount(matchTable, opOwner) != 0) {
//                    ServerLoggers.assertLog(false, "TEMPORARY TABLE NOT EMPTY");
//                    session.truncateSession(matchTable, opOwner, TableOwner.none);
//                }
                // падает, когда происходит, создание таблицы за пределами транзакции, затем внутри делается drop, и не делается rollDrop - зависли хинты (си. clearHints), или асинхронно кто-то влазит в транзакцию
                ServerLoggers.assertLog(!Settings.get().isCheckSessionCount() || session.getSessionCount(matchTable, opOwner) == 0, "SESSION TABLE SHOULD BE EMPTY AT CACHE"); // !!! после used потому как выполняет sql, и например может выполнится tryCommon который вернет privateConnection
//                SQLSession.addUsed(matchTable, owner, used, usedStacks);
                isNew.set(false);
                CacheStats.incrementHit(CacheStats.CacheType.TEMP_TABLE);
                return matchTable;
            }

        // если нет, "создаем" таблицу
        CacheStats.incrementMissed(CacheStats.CacheType.TEMP_TABLE);
        String table = getTableName(counter);
        assert !used.containsKey(table);
        used.put(table, new WeakReference<>(owner)); // до всех sql, см. выше
        debugInfo.put(table, owner.getDebugInfo());
        session.createTemporaryTable(table, keys, properties, opOwner);
        counter++;
        //        SQLSession.addUsed(table, owner, used, usedStacks);
        matchTables.add(table);
        isNew.set(true);
        structs.put(table, fieldStruct);
        return table;
    }

    public String getTableName(int count) {
        return "t_" + count;
    }

    // assert synchronized
    public Set<String> getTables() {
        return structs.keySet();
    }

    // assert synchronized
    public FieldStruct getStruct(String table) {
        return structs.get(table);
    }

    // lockRead, нет temporaryTables.lock
    public void fillData(SQLSession session, FillTemporaryTable data, Integer count, Result<Integer> resultActual, String table, OperationOwner owner) throws SQLException, SQLHandledException {

        Integer actual = data.fill(table); // заполняем
        assert (actual!=null)==(count==null);
        if(session.syntax.supportsAnalyzeSessionTable()) {
            if (Settings.get().isAutoAnalyzeTempStats())
                session.vacuumAnalyzeSessionTable(table, owner);
            else {
                assert false; // ??? с синхронизацией stats
                Object actualStatistics = getDBStatistics(actual);
                Object currentStat = stats.get(table);
                if (!actualStatistics.equals(currentStat)) {
                    session.vacuumAnalyzeSessionTable(table, owner);
                    stats.put(table, actualStatistics);
                }
            }
        }
        if(count==null)
            resultActual.set(actual);
        else
            resultActual.set(count);
    }

    @AssertSynchronized
    public void removeTable(String table) { // SQLSession.assertLock - либо temporaryTables.lock() + lockRead либо lockWrite
        if(!Settings.get().isAutoAnalyzeTempStats())
            stats.remove(table);
        FieldStruct fieldStruct = structs.remove(table);
        Set<String> structTables = tables.get(fieldStruct);
        structTables.remove(table);
        if(structTables.isEmpty())
            tables.remove(fieldStruct);
    }

    public static class FieldStruct {

        public final ImOrderSet<KeyField> keys;
        public final ImSet<PropertyField> properties;

        private final Object statistics;

        public FieldStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count) {
            this.keys = keys;
            this.properties = properties;

            if(Settings.get().isAutoAnalyzeTempStats() || count==null)
                this.statistics = null;
            else
                this.statistics = getDBStatistics(count);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof FieldStruct && keys.equals(((FieldStruct) o).keys) && properties.equals(((FieldStruct) o).properties) && BaseUtils.nullEquals(statistics, ((FieldStruct) o).statistics);

        }

        @Override
        public int hashCode() {
            return 31 * (31 * keys.hashCode() + properties.hashCode()) + BaseUtils.nullHash(statistics);
        }
    }

    public static Object getDBStatistics(int count) {
        return new Stat(count);
    }
}
