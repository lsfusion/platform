package lsfusion.server.data.query.innerjoins;

import lsfusion.base.BaseUtils;
import lsfusion.base.ImmutableObject;
import lsfusion.base.SymmPair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.PackInterface;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.*;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;

import java.util.*;

// используется только в groupJoinWheres, по сути protected класс
public class GroupJoinsWheres extends DNFWheres<WhereJoins, GroupJoinsWheres.Value, GroupJoinsWheres> implements PackInterface<GroupJoinsWheres> {

    public static enum Type {
        WHEREJOINS, STAT_WITH_WHERE, STAT_ONLY;
        
        public boolean noWhere() {
            return this == STAT_ONLY;            
        }
        
        public boolean isStat() {
            return this != WHEREJOINS;
        }
    }
    
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

        public Value(WhereJoin join, Where where) {
            this(MapFact.<WhereJoin, Where>singleton(join, where), where);
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
            assert valueKey.means(key);
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

    private static <K extends BaseExpr> boolean compileMeans(WhereJoins from, WhereJoins what, ImSet<K> keepStat, KeyStat keyStat, boolean saveStat) {
        return from.means(what) && (!saveStat || BaseUtils.hashEquals(from.getCompileStatKeys(keepStat, keyStat), what.getCompileStatKeys(keepStat, keyStat)));
    }

    public GroupJoinsWheres(ImMap<WhereJoins, Value> map, boolean noWhere) {
        super(map);
        this.noWhere = noWhere;
    }

    public GroupJoinsWheres(ImMap<WhereJoins, Value> map, Type type) {
        this(map, type.noWhere());
    }

    public <K extends BaseExpr> GroupJoinsWheres pack(ImSet<K> keepStat, KeyStat keyStat, Type type, Where where, boolean intermediate, ImOrderSet<Expr> orderTop) {
        assert !intermediate || isExceededIntermediatePackThreshold();
        if(intermediate && type.isStat()) // самый быстрый способ сохранить статистику, проверка на intermediate чтобы не было рекурсии
            return new GroupJoinsWheres(new StatKeysJoin<K>(getStatKeys(keepStat, keyStat)), where, type);
        GroupJoinsWheres result = pack(keepStat, keyStat, type.isStat()); // savestat нужно для более правильной статистикой 
//        GroupJoinsWheres result = packMeans(keepStat, keyStat, intermediate);
        if(result.size() == 1) { // оптимизация
            Value value = result.singleValue();
            if(!BaseUtils.hashEquals(value.where, where)) {
                assert !orderTop.isEmpty() || (value.where.means(where) && where.means(value.where)) || where.hasUnionExpr(); // hasUnionExpr - через getCommonWhere может залазить внутрь UnionExpr и терять "следствия" тем самым, !orderTop.isEmpty из-за symmetricWhere в groupNotJoinsWheres
                if(value.where.getComplexity(false) < where.getComplexity(false))
                    where = value.where;
                result = new GroupJoinsWheres(result.singleKey(), new Value(value.upWheres, where), type);
            }
        }
        return result; 
    }
    
    // проверка превышения промежуточного порога 
    public boolean isExceededIntermediatePackThreshold() {
        int degree = Settings.get().getLimitWhereJoinsDegree();
        return size() > Settings.get().getLimitWhereJoinsCount() * degree || getComplexity(true) > Settings.get().getLimitWhereJoinsComplexity() * degree;
    }

    public boolean isFitPackThreshold() {
        return !(size() > Settings.get().getLimitWhereJoinsCount() || (!noWhere && getComplexity(true) > Settings.get().getLimitWhereJoinsComplexity()));
    }
    
    private <K extends BaseExpr> GroupJoinsWheres pack(ImSet<K> keepStat, KeyStat keyStat, boolean saveStat) {
        int forcePack = Settings.get().getForceWhereJoinsPack();

        GroupJoinsWheres result = this;
        if(forcePack <= 0 && result.isFitPackThreshold())
            return result;
        
        result = result.packMeans(keepStat, keyStat, true);
        if(forcePack <= 1 && result.isFitPackThreshold())
            return result;
        
        if(!saveStat) {
            result = result.packMeans(keepStat, keyStat, false); // оптимизация, так как packMeans быстрее packReduce
            if (result.isFitPackThreshold())
                return result;
        }
  
        return result.packReduce(keepStat, keyStat, saveStat); // !!! result.packReduce 
    }

    // keepStat нужен чтобы можно было гарантировать что не образуется case с недостающим WhereJoin существенно влияющим на статистику
    private <K extends BaseExpr> GroupJoinsWheres packMeans(ImSet<K> keepStat, KeyStat keyStat, boolean saveStat) {
        if(!Settings.get().isCompileMeans())
            return this;

        Map<WhereJoins, Value> result = MapFact.mAddRemoveMap(); // remove есть
        for(int i=0,size=size();i<size;i++) {
            WhereJoins objectJoin = getKey(i);
            Value where = getValue(i);

            boolean found = false;
            // ищем кого-нибудь кого он means
            for(Map.Entry<WhereJoins, Value> resultJoin : result.entrySet())
                if(compileMeans(objectJoin, resultJoin.getKey(), keepStat, keyStat, saveStat)) {
                    resultJoin.setValue(resultJoin.getValue().orMeans(resultJoin.getKey(), objectJoin, where, noWhere));
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                for(Iterator<Map.Entry<WhereJoins,Value>> it = result.entrySet().iterator();it.hasNext();) {
                    Map.Entry<WhereJoins, Value> resultJoin = it.next();
                    if(compileMeans(resultJoin.getKey(), objectJoin, keepStat, keyStat, saveStat)) {
                        where = where.orMeans(objectJoin, resultJoin.getKey(), resultJoin.getValue(), noWhere);
                        it.remove();
                    }
                }
                result.put(objectJoin, where);
            }
        }

        return new GroupJoinsWheres(MapFact.fromJavaMap(result), noWhere);
    }
    
    private abstract static class CEntry extends ImmutableObject {
        public final WhereJoins where;
        public final int rows;
        public final int orderTopCount;
        public final int childrenCount;

        public <K extends BaseExpr> CEntry(WhereJoins where, ImSet<K> keepStat, KeyStat keyStat) {
            this.where = where;
            
            rows = where.getCompileStatKeys(keepStat, keyStat).rows.getWeight();
            orderTopCount = where.getOrderTopCount();
            childrenCount = where.getAllChildrenCount();
        }
        
        public abstract void fillOriginal(MExclSet<WhereJoins> wheres);
    }
    
    private static class COriginal extends CEntry {
        public <K extends BaseExpr> COriginal(WhereJoins where, ImSet<K> keepStat, KeyStat keyStat) {
            super(where, keepStat, keyStat);
        }

        public void fillOriginal(MExclSet<WhereJoins> wheres) {
            wheres.exclAdd(where);
        }
    }    
    
    // минимум изменения, абсолютной статистики, количества сршдвкутэjd    
    private static long getPriority(int r, int m, int oc, int c) {
        return (((long)r) * Stat.MAX.getWeight() + (r == 0 ? 0 : m)) * 100l + oc * 100 + c;
    }

    private static long getMaxPriority() {
        return getPriority(Stat.AGGR.getWeight(), 0, 0, 0);
    }

    private static class CMerged extends CEntry implements Comparable<CMerged> {
        public final SymmPair<CEntry, CEntry> original;

        public <K extends BaseExpr> CMerged(SymmPair<CEntry, CEntry> original, WhereJoins where, ImSet<K> keepStat, KeyStat keyStat) {
            super(where, keepStat, keyStat);
            this.original = original;

//            assert assertMeansOriginal(where);
        }

//        private boolean assertMeansOriginal(WhereJoins where) {
//            MExclSet<WhereJoins> mOrigs = SetFact.mExclSet();
//            fillOriginal(mOrigs);
//
//            for(WhereJoins orig : mOrigs.immutable()) {
//                assert orig.means(where);
//            }
//            return true;
//        }

        protected int getRowDiff() {
            int w1 = original.first.rows;
            int w2 = original.second.rows;
            assert rows >= w1 && rows >= w2; // возможно как и в pushWhere будет нарушаться
            return BaseUtils.min(rows - w1, rows - w2);
        }

        protected long getPriority() {
            int cm = childrenCount;
            int c1 = original.first.childrenCount;
            int c2 = original.second.childrenCount;
            assert cm <= c1 && cm <= c2;

            int ocm = orderTopCount;
            int oc1 = original.first.orderTopCount;
            int oc2 = original.second.orderTopCount;
            assert ocm <= oc1 && ocm <= oc2;

            return GroupJoinsWheres.getPriority(getRowDiff(), rows, BaseUtils.max(oc1 - ocm, oc2 - ocm), BaseUtils.min(c1 - cm, c2 - cm));
        }
        
        @Override
        public int compareTo(CMerged o) {
            return Long.compare(getPriority(), o.getPriority());
        }

        public void fillOriginal(MExclSet<WhereJoins> wheres) {
            original.first.fillOriginal(wheres);
            original.second.fillOriginal(wheres);
        }
    }
    
    // эвристика с приоритезацией
    private <K extends BaseExpr> GroupJoinsWheres packReduce(ImSet<K> keepStat, KeyStat keyStat, boolean saveStat) {
        if(!Settings.get().isCompileMeans())
            return this;

        int limit = Settings.get().getLimitWhereJoinsCount(); // пока только на count смотрим, так как complexity высчитываем в конце
        long maxPriority = getMaxPriority();

        // (A, B) -> X, Heap по X

        // ?? кэшировать getStatKeys() ??? getAllChildren !!! merge getAllChildren.count - min getStatKeys - в рамках контекста ??? хотя конечно можно включить и глобальное кэшировние с быстрой очисткой
        // изменение для пары : min(a,b) - x;
        // фильтрация (увеличение статистики = 0, если enableStat, иначе что статистика less ALOT)
        // приоритет увеличение статистики, уменьшение числа элементов (? от изначального или текущего ???
        
        // оборачиваем в c* так как могут начать повторятся
        PriorityQueue<CMerged> priority = new PriorityQueue<CMerged>();
        Map<SymmPair<CEntry, CEntry>, CMerged> matrix = MapFact.mAddRemoveMap();
        Set<CEntry> current = SetFact.mAddRemoveSet();
        
        // бежим по всем A, по все B добавляем (с фильтрацией) в heap и матрицу (A, B) -> A OR B
        for(int i=0,size=size();i<size;i++) {
            current.add(new COriginal(getKey(i), keepStat, keyStat));
        }
        List<CEntry> list = new ArrayList<CEntry>(current);
        for(int i=0,size=list.size();i<size;i++) {
            CEntry iJoin = list.get(i);
            for(int j=i+1;j<size;j++) {
                CEntry jJoin = list.get(j);
                
                SymmPair<CEntry, CEntry> pair = new SymmPair<CEntry, CEntry>(iJoin, jJoin);
                CMerged cEntry = new CMerged(pair, iJoin.where.or(jJoin.where), keepStat, keyStat);
                priority.add(cEntry);
                matrix.put(pair, cEntry);
            }
        }
        
        // пока heap не пуста (из-за фильтрации), в ней верхний  0'е элементы или количество элементов больше порога,
            // достаем из heap X + (A,B), 
            // для всех C<>A и C<>B удаляем из heap и матрицы (A, C) -> Xa, и (B, C) -> Xb, добавляем (с фильтрацией) в heap и матрицу (X, C) -> Xa OR Xb, 
        while(true) {
            if(priority.isEmpty()) { // остался максимум один элемент
                assert current.size() <= 1;
                break;
            }
            
            CMerged entry = priority.poll();

            if(saveStat && entry.getRowDiff() > 0)
                break;
            long currentPriority = entry.getPriority();
            if(currentPriority >= maxPriority)
                break;
            if(currentPriority != 0 && current.size() <= limit) { // проверка на не !=0 имеет мало смысла так как все равно packMeans есть до (перенести этот коммент к minprioirity)
                break;
            }
            
            current.remove(entry.original.first);
            current.remove(entry.original.second);
            
            for(CEntry element : current) {
                SymmPair<CEntry, CEntry> firstPair = new SymmPair<CEntry, CEntry>(entry.original.first, element);
                SymmPair<CEntry, CEntry> secondPair = new SymmPair<CEntry, CEntry>(entry.original.second, element);
                CMerged firstEntry = matrix.remove(firstPair);
                CMerged secondEntry = matrix.remove(secondPair);
                priority.remove(firstEntry);
                priority.remove(secondEntry);

                SymmPair<CEntry, CEntry> newEntry = new SymmPair<CEntry, CEntry>(entry, element);
                CMerged merged = new CMerged(newEntry, firstEntry.where.or(secondEntry.where), keepStat, keyStat);
                matrix.put(newEntry, merged);
                priority.add(merged);
            }
            
            current.add(entry);
        }

        // бежим по оставшимся, разворачиваем по парам до множества исходных элементов для всех делаем or (A orMeans X), это и будет результат
        return new GroupJoinsWheres(SetFact.fromJavaSet(current).mapKeyValues(new GetValue<WhereJoins, CEntry>() {
            public WhereJoins getMapValue(CEntry value) {
                return value.where;
            }}, new GetValue<Value, CEntry>() {
            public Value getMapValue(CEntry entry) {
                if(entry instanceof COriginal) // оптимизация
                    return get(entry.where);

                MExclSet<WhereJoins> mOrigs = SetFact.mExclSet();
                entry.fillOriginal(mOrigs);
                
                Value resultValue = new Value(SetFact.toExclSet(entry.where.wheres).toMap(Where.FALSE), Where.FALSE);
                for(WhereJoins orig : mOrigs.immutable()) {
                    assert orig.means(entry.where);
                    Value origValue = get(orig);
                    resultValue = resultValue.orMeans(entry.where, orig, origValue, noWhere);
                }                
                return resultValue;
            }}), noWhere);
    }

    public void fillList(KeyEqual keyEqual, MCol<GroupJoinsWhere> col, ImOrderSet<Expr> orderTop) {
        for(int i=0,size=size();i<size;i++) {
            Value value = getValue(i);
            col.add(new GroupJoinsWhere(keyEqual, getKey(i), value.upWheres, value.where, orderTop));
        }
    }
    
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> keepStat, KeyStat keyStat) {
        StatKeys<K> result = new StatKeys<K>(keepStat);
        for(WhereJoins whereJoins : keyIt())
            result = result.or(whereJoins.getStatKeys(keepStat, keyStat));
        return result;    
    }

    private final boolean noWhere;
    private GroupJoinsWheres(WhereJoins inner, Value where, Type type) {
        super(inner, where);
        this.noWhere = type.noWhere();
    }

    public GroupJoinsWheres(Where where, Type type) {
        this(WhereJoins.EMPTY, new Value(where), type);
    }

    public GroupJoinsWheres(WhereJoin join, Where where, Type type) {
        this(new WhereJoins(join), new Value(join, where), type);
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
