package lsfusion.server.data.query.innerjoins;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.Where;

import java.util.Collection;
import java.util.Iterator;

// используется только в SPLIT'е, да и при текущих оптимизациях де-факто не используется
public enum GroupStatType {
    ALL, STAT, NONE;

    // группируем по KeyEqual + статистике, or'им Where
    private <K> ImCol<GroupSplitWhere<K>> groupStat(boolean noWhere, ImCol<GroupSplitWhere<K>>... statJoinsList) {

        MMap<Pair<KeyEqual, StatKeys<K>>, Where> mMapWhere = MapFact.mMap(AbstractWhere.<Pair<KeyEqual, StatKeys<K>>>addOr());
        for(ImCol<GroupSplitWhere<K>> statJoins : statJoinsList)
            for(GroupSplitWhere<K> statJoin : statJoins)
                mMapWhere.add(new Pair<>(statJoin.keyEqual,
                        statJoin.stats), noWhere ? Where.TRUE : statJoin.where);
        ImMap<Pair<KeyEqual, StatKeys<K>>, Where> mapWhere = mMapWhere.immutable();

        MExclSet<GroupSplitWhere<K>> mResult = SetFact.mExclSet(mapWhere.size());
        for(int i=0,size=mapWhere.size();i<size;i++) { // возвращаем результат
            Pair<KeyEqual, StatKeys<K>> map = mapWhere.getKey(i);
            mResult.exclAdd(new GroupSplitWhere<>(map.first, map.second, mapWhere.getValue(i)));
        }
        return mResult.immutable();
    }

    // группируем по keyEqual, or'им StatKeys и Where
    private <K> ImCol<GroupSplitWhere<K>> groupAll(boolean noWhere, ImMap<KeyEqual, Where> keyEquals, ImCol<GroupSplitWhere<K>>... statJoinsList) {

        MMap<KeyEqual, Where> mMapWhere = MapFact.mMap(AbstractWhere.<KeyEqual>addOr());
        MMap<KeyEqual, StatKeys<K>> mMapStats = MapFact.mMap(StatKeys.<KeyEqual, K>addOr());
        for(ImCol<GroupSplitWhere<K>> statJoins : statJoinsList)
            for(GroupSplitWhere<K> statJoin : statJoins) {
                mMapWhere.add(statJoin.keyEqual, noWhere || keyEquals!=null ? Where.TRUE : statJoin.where);
                mMapStats.add(statJoin.keyEqual, statJoin.stats);
            }
        ImMap<KeyEqual, Where> mapWhere = mMapWhere.immutable();
        ImMap<KeyEqual, StatKeys<K>> mapStats = mMapStats.immutable();

        MExclSet<GroupSplitWhere<K>> mResult = SetFact.mExclSet(mapWhere.size());
        for(int i=0,size=mapWhere.size();i<size;i++) { // возвращаем результат
            KeyEqual keys = mapWhere.getKey(i);
            mResult.exclAdd(new GroupSplitWhere<>(keys, mapStats.get(keys), keyEquals != null ? keyEquals.get(keys) : mapWhere.getValue(i)));
        }
        return mResult.immutable();

    }

    public <K> ImCol<GroupSplitWhere<K>> group(ImCol<GroupSplitWhere<K>> statJoins, boolean noWhere, ImMap<KeyEqual, Where> keyEquals) { // statJoins - в NONE группировке
        switch(this) {
            case NONE:
                return statJoins;
            case STAT:
                return groupStat(noWhere, statJoins);
            case ALL:
                return groupAll(noWhere, keyEquals, statJoins);
        }
        throw new RuntimeException("should not be");
    }
    
    public <K> ImCol<GroupSplitWhere<K>> merge(ImCol<GroupSplitWhere<K>> joins1, ImCol<GroupSplitWhere<K>> joins2, boolean noWhere) { // joins1, joins2 в this группировке
        switch(this) {
            case NONE:
                return joins1.mergeCol(joins2);
            case STAT:
                return groupStat(noWhere, joins1, joins2);
            case ALL:
                return groupAll(noWhere, null, joins1, joins2);
        }
        throw new RuntimeException("should not be");
    }

    public <K> ImCol<GroupSplitWhere<K>> merge(ImCol<GroupSplitWhere<K>> mergeJoins, GroupSplitWhere<K> join) { // joins1 в this группировке
        // для оптимизации сделаем for'ом
        Collection<GroupSplitWhere<K>> result = ListFact.mAddRemoveCol();
        ListFact.addJavaAll(mergeJoins, result);

        switch(this) {
            case NONE:
                break;
            case STAT: {
                for(Iterator<GroupSplitWhere<K>> i = result.iterator(); i.hasNext();) {
                    GroupSplitWhere<K> where = i.next();
                    if(where.keyEqual.equals(join.keyEqual) && where.stats.equals(join.stats)) {
                        i.remove();
                        join = new GroupSplitWhere<>(join.keyEqual, join.stats, join.where.or(where.where));
                        break;
                    }
                }
                break;
            } case ALL:
                for(Iterator<GroupSplitWhere<K>> i = result.iterator(); i.hasNext();) {
                    GroupSplitWhere<K> where = i.next();
                    if(where.keyEqual.equals(join.keyEqual)) {
                        i.remove();
                        join = new GroupSplitWhere<>(join.keyEqual, join.stats.or(where.stats), join.where.or(where.where));
                        break;
                    }
                }
                break;
            default:
                throw new RuntimeException("should not be");
        }
        result.add(join);
        return ListFact.fromJavaCol(result);
    }
}
