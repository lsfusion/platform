package platform.server.data.query.innerjoins;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.PackInterface;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;
import platform.server.Settings;

import java.util.*;

// используется только в groupJoinWheres, по сути protected класс
public class GroupJoinsWheres extends DNFWheres<WhereJoins, GroupJoinsWheres.Value, GroupJoinsWheres> implements PackInterface<GroupJoinsWheres> {

    public static class Value {
        public final Map<WhereJoin, Where> upWheres; // впоследствии только для проталкивания
        public final Where where;

        public Value(Where where) {
            this(new HashMap<WhereJoin, Where>(), where);
        }

        public Value(WhereJoin join, DataWhere where) {
            this(Collections.singletonMap(join, (Where)where), where);
        }

        public Value(Map<WhereJoin, Where> upWheres, Where where) {
            this.upWheres = upWheres;
            this.where = where;
        }

        public Value and(WhereJoins key, Value value) {
            return new Value(key.andUpWheres(upWheres, value.upWheres), where.and(value.where));
        }

        public Value or(WhereJoins key, Value value) {
            return new Value(key.orUpWheres(upWheres, value.upWheres), where.or(value.where));
        }

        // or , но с assertion'ом что из этого WhereJoins следует WhereJoins сливаемый
        public Value orMeans(WhereJoins key, WhereJoins valueKey,Value value) {
            return new Value(key.orMeanUpWheres(upWheres, valueKey, value.upWheres), where.or(value.where));
        }
    }

    protected Value andValue(WhereJoins key, Value prevValue, Value newValue) {
        return prevValue.and(key, newValue);
    }

    protected Value addValue(WhereJoins key, Value prevValue, Value newValue) {
        return prevValue.or(key, newValue);
    }

    protected boolean valueIsFalse(Value value) {
        return value.where.isFalse();
    }

    protected GroupJoinsWheres createThis() {
        return new GroupJoinsWheres();
    }

    private static <K extends BaseExpr> boolean compileMeans(WhereJoins from, WhereJoins what, QuickSet<K> keepStat, KeyStat keyStat) {
        return from.means(what) && (keepStat == null || BaseUtils.hashEquals(from.getStatKeys(keepStat, keyStat), what.getStatKeys(keepStat, keyStat)));
    }

    private GroupJoinsWheres(Map<WhereJoins, Value> map) {
        for(Map.Entry<WhereJoins, Value> entry : map.entrySet())
            add(entry.getKey(), entry.getValue());
    }

    // keepStat нужен чтобы можно было гарантировать что не образуется case с недостающим WhereJoin существенно влияющим на статистику
    public <K extends BaseExpr> GroupJoinsWheres compileMeans(QuickSet<K> keepStat, KeyStat keyStat) {
        if(!Settings.instance.isCompileMeans())
            return this;

        Map<WhereJoins, Value> result = new HashMap<WhereJoins, Value>();
        for(int i=0;i<size;i++) {
            WhereJoins objectJoin = getKey(i);
            Value where = getValue(i);

            boolean found = false;
            // ищем кого-нибудь кого он means
            for(Map.Entry<WhereJoins, Value> resultJoin : result.entrySet())
                if(compileMeans(objectJoin, resultJoin.getKey(), keepStat, keyStat)) {
                    resultJoin.setValue(resultJoin.getValue().orMeans(resultJoin.getKey(), objectJoin, where));
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                for(Iterator<Map.Entry<WhereJoins,Value>> it = result.entrySet().iterator();it.hasNext();) {
                    Map.Entry<WhereJoins, Value> resultJoin = it.next();
                    if(compileMeans(resultJoin.getKey(), objectJoin, keepStat, keyStat)) {
                        where = where.orMeans(objectJoin, resultJoin.getKey(), resultJoin.getValue());
                        it.remove();
                    }
                }
                result.put(objectJoin, where);
            }
        }

        return new GroupJoinsWheres(result);
    }

    public GroupJoinsWheres compileMeans() {
        return compileMeans(null, null);
    }

    public void fillList(KeyEqual keyEqual, Collection<GroupJoinsWhere> col) {
        for(int i=0;i<size;i++) {
            Value value = getValue(i);
            col.add(new GroupJoinsWhere(keyEqual, getKey(i), value.upWheres, value.where));
        }
    }

    public GroupJoinsWheres() {
    }

    private GroupJoinsWheres(WhereJoins inner, Value where) {
        super(inner, where);
    }

    public GroupJoinsWheres(Where where) {
        this(new WhereJoins(), new Value(where));
    }

    public GroupJoinsWheres(WhereJoin join, DataWhere where) {
        this(new WhereJoins(join), new Value(join, where));
    }

    public GroupJoinsWheres pack() {
        throw new RuntimeException("not supported yet");
    }

    public long getComplexity(boolean outer) {
        int result = 0;
        for(int i=0;i<size;i++)
            result += getValue(i).where.getComplexity(outer);
        return result;
    }
}
