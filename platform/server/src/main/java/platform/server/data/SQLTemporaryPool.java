package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Result;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;

// выделяется в отдельный объект так как синхронизироваться должен
public class SQLTemporaryPool {
    private final Map<FieldStruct, Set<String>> tables = new HashMap<FieldStruct, Set<String>>();
    private final Map<FieldStruct, Map<String, Object>> statistics = new HashMap<FieldStruct, Map<String, Object>>(); // статистика для FieldStruct'ов без таковой
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
                        Object actualStatistics = getCountStatistics(actual);
                        Map<String, Object> structStatistics = statistics.get(fieldStruct);
                        if(!actualStatistics.equals(structStatistics.get(matchTable))) {
                            session.analyzeSessionTable(matchTable);
                            structStatistics.put(matchTable, actualStatistics);
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
                Map<String, Object> structStatistics = statistics.get(fieldStruct);
                if(structStatistics==null) {
                    structStatistics = new HashMap<String, Object>();
                    statistics.put(fieldStruct, structStatistics);
                }
                structStatistics.put(table, getCountStatistics(actual));
                resultActual.set(actual);
            }

            matchTables.add(table);

            return table;
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
                this.statistics = getCountStatistics(count);
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

    public static Object getCountStatistics(int count) {
        return 1;
    }
}
