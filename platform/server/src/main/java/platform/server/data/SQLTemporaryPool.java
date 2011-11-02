package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.data.expr.query.Stat;
import sun.font.StandardTextSource;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;

// выделяется в отдельный объект так как синхронизироваться должен
public class SQLTemporaryPool {
    private final Map<FieldStruct, Set<String>> tables = new HashMap<FieldStruct, Set<String>>();
    private final Map<String, Object> stats = new HashMap<String, Object>();
    private final Map<String, FieldStruct> structs = new HashMap<String, FieldStruct>(); // чтобы удалять таблицы, не имея структры
    private int counter = 0;

    public boolean isEmpty(Map<String, WeakReference<Object>> used) {
        return tables.isEmpty();
    }

    public String getTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, FillTemporaryTable data, Integer count, Result<Integer> resultActual, Map<String, WeakReference<Object>> used) throws SQLException {
        FieldStruct fieldStruct = new FieldStruct(keys, properties, count);
        resultActual.set(count);

        synchronized(tables) {
            Set<String> matchTables = tables.get(fieldStruct);
            if(matchTables==null) {
                matchTables = new HashSet<String>();
                tables.put(fieldStruct, matchTables);
            }

            for(String matchTable : matchTables) // ищем нужную таблицу
                if(!used.containsKey(matchTable)) { // если не используется
                    session.truncate(matchTable); // удаляем старые данные
                    Integer actual = data.fill(matchTable); // заполняем
                    assert (actual!=null)==(count==null);
                    if(count==null) {
                        Object actualStatistics = getDBStatistics(actual);
                        if(!actualStatistics.equals(stats.get(matchTable))) {
                            session.analyzeSessionTable(matchTable);
                            stats.put(matchTable, actualStatistics);
                        }
                        resultActual.set(actual);
                    }
                    return matchTable;
                }

            // если нет, "создаем" таблицу
            String table = "t_" + (counter++);
            session.createTemporaryTable(table, keys, properties);
            Integer actual = data.fill(table); // заполняем
            assert (actual!=null)==(count==null);
            session.analyzeSessionTable(table); // выполняем ее analyze, чтобы сказать постгре какие там будут записи
            if(count==null) { // обновляем реальной статистикой
                stats.put(table, getDBStatistics(actual));
                resultActual.set(actual);
            }
            structs.put(table, fieldStruct);

            matchTables.add(table);

            return table;
        }
    }

    public void removeTable(String table) {
        synchronized (tables) {
            stats.remove(table);
            FieldStruct fieldStruct = structs.remove(table);
            tables.get(fieldStruct).remove(table);
        }
    }

    private static class FieldStruct {

        public final List<KeyField> keys;
        public final Collection<PropertyField> properties;

        private final Object statistics;

        public FieldStruct(List<KeyField> keys, Collection<PropertyField> properties, Integer count) {
            this.keys = keys;
            this.properties = properties;

            if(count==null)
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
