package lsfusion.server.data.query.innerjoins;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.SimpleAddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.PackInterface;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.ObjectWhere;
import lsfusion.server.data.where.Where;

import java.util.Iterator;
import java.util.Map;

// используется только в groupJoinWheres, по сути protected класс
public class GroupJoinsWheres extends DNFWheres<WhereJoins, GroupJoinsWheres.Value, GroupJoinsWheres> implements PackInterface<GroupJoinsWheres> {

    private static class WriteMap extends SymmAddValue<WhereJoins, Value> {
        private final boolean noWhere;

        public WriteMap(boolean noWhere) {
            this.noWhere = noWhere;
        }

        public Value addValue(WhereJoins key, Value prevValue, Value newValue) {
            return prevValue.or(key, newValue, noWhere);
        }
    }
    private static final WriteMap addValue = new WriteMap(false);
    private static final WriteMap addValueNoWhere = new WriteMap(true);

    public static AddValue<WhereJoins, Value> getAddValue(boolean noWhere) {
        if(noWhere)
            return addValueNoWhere;
        else
            return addValue;
    }

    @Override
    protected AddValue<WhereJoins, Value> getAddValue() {
        return getAddValue(noWhere);
    }

    public static class Value {
        public final ImMap<WhereJoin, Where> upWheres; // впоследствии только для проталкивания
        public final Where where;

        public Value(Where where) {
            this(MapFact.<WhereJoin, Where>EMPTY(), where);
        }

        public Value(WhereJoin join, ObjectWhere where) {
            this(MapFact.<WhereJoin, Where>singleton(join, (Where) where), where);
        }

        public Value(ImMap<WhereJoin, Where> upWheres, Where where) {
            this.upWheres = upWheres;
            this.where = where;
        }

        public Value and(WhereJoins key, Value value, boolean noWhere) {
            return new Value(key.andUpWheres(upWheres, value.upWheres), noWhere ? (Where)where.andCheck(value.where) : where.and(value.where));
        }

        public Value or(WhereJoins key, Value value, boolean noWhere) {
            return new Value(key.orUpWheres(upWheres, value.upWheres), noWhere ? (Where)where.orCheck(value.where) : where.or(value.where));
        }

        // or , но с assertion'ом что из этого WhereJoins следует WhereJoins сливаемый
        public Value orMeans(WhereJoins key, WhereJoins valueKey, Value value, boolean noWhere) {
            return new Value(key.orMeanUpWheres(upWheres, valueKey, value.upWheres), noWhere ? (Where)where.orCheck(value.where) : where.or(value.where));
        }
    }

    protected Value andValue(WhereJoins key, Value prevValue, Value newValue) {
        return prevValue.and(key, newValue, noWhere);
    }

    protected boolean valueIsFalse(Value value) {
        return noWhere ? value.where.not().checkTrue() : value.where.isFalse();
    }

    protected GroupJoinsWheres createThis(ImMap<WhereJoins, Value> map) {
        return new GroupJoinsWheres(map, noWhere);
    }

    private static <K extends BaseExpr> boolean compileMeans(WhereJoins from, WhereJoins what, ImSet<K> keepStat, KeyStat keyStat) {
        return from.means(what) && (keepStat == null || BaseUtils.hashEquals(from.getStatKeys(keepStat, keyStat), what.getStatKeys(keepStat, keyStat)));
    }

    public GroupJoinsWheres(ImMap<WhereJoins, Value> map, boolean noWhere) {
        super(map);
        this.noWhere = noWhere;
    }

    // keepStat нужен чтобы можно было гарантировать что не образуется case с недостающим WhereJoin существенно влияющим на статистику
    public <K extends BaseExpr> GroupJoinsWheres compileMeans(ImSet<K> keepStat, KeyStat keyStat) {
        if(!Settings.get().isCompileMeans())
            return this;

        Map<WhereJoins, Value> result = MapFact.mAddRemoveMap(); // remove есть
        for(int i=0,size=size();i<size;i++) {
            WhereJoins objectJoin = getKey(i);
            Value where = getValue(i);

            boolean found = false;
            // ищем кого-нибудь кого он means
            for(Map.Entry<WhereJoins, Value> resultJoin : result.entrySet())
                if(compileMeans(objectJoin, resultJoin.getKey(), keepStat, keyStat)) {
                    resultJoin.setValue(resultJoin.getValue().orMeans(resultJoin.getKey(), objectJoin, where, noWhere));
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                for(Iterator<Map.Entry<WhereJoins,Value>> it = result.entrySet().iterator();it.hasNext();) {
                    Map.Entry<WhereJoins, Value> resultJoin = it.next();
                    if(compileMeans(resultJoin.getKey(), objectJoin, keepStat, keyStat)) {
                        where = where.orMeans(objectJoin, resultJoin.getKey(), resultJoin.getValue(), noWhere);
                        it.remove();
                    }
                }
                result.put(objectJoin, where);
            }
        }

        return new GroupJoinsWheres(MapFact.fromJavaMap(result), noWhere);
    }

    public GroupJoinsWheres compileMeans() {
        return compileMeans(null, null);
    }

    public void fillList(KeyEqual keyEqual, MCol<GroupJoinsWhere> col) {
        for(int i=0,size=size();i<size;i++) {
            Value value = getValue(i);
            col.add(new GroupJoinsWhere(keyEqual, getKey(i), value.upWheres, value.where));
        }
    }

    private final boolean noWhere;
    private GroupJoinsWheres(WhereJoins inner, Value where, boolean noWhere) {
        super(inner, where);
        this.noWhere = noWhere;
    }

    public GroupJoinsWheres(Where where, boolean noWhere) {
        this(new WhereJoins(), new Value(where), noWhere);
    }

    public GroupJoinsWheres(WhereJoin join, ObjectWhere where, boolean noWhere) {
        this(new WhereJoins(join), new Value(join, where), noWhere);
    }

    public GroupJoinsWheres pack() {
        throw new RuntimeException("not supported yet");
    }

    public long getComplexity(boolean outer) {
        int result = 0;
        for(int i=0,size=size();i<size;i++)
            result += getValue(i).where.getComplexity(outer);
        return result;
    }
}
