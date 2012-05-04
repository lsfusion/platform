package platform.server.data.query.innerjoins;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickMap;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.MapStatKeys;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public enum GroupStatType {
    ALL, STAT, NONE;

    // группируем по KeyEqual + статистике, or'им Where
    private <K> Collection<GroupStatWhere<K>> groupStat(boolean noWhere, Collection<GroupStatWhere<K>>... statJoinsList) {
        Collection<GroupStatWhere<K>> result = new ArrayList<GroupStatWhere<K>>();
        MapWhere<Pair<KeyEqual, StatKeys<K>>> mapWhere = new MapWhere<Pair<KeyEqual, StatKeys<K>>>();
        for(Collection<GroupStatWhere<K>> statJoins : statJoinsList)
            for(GroupStatWhere<K> statJoin : statJoins)
                mapWhere.add(new Pair<KeyEqual, StatKeys<K>>(statJoin.keyEqual,
                        statJoin.stats), noWhere ? Where.TRUE : statJoin.where);
        for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
            Pair<KeyEqual, StatKeys<K>> map = mapWhere.getKey(i);
            result.add(new GroupStatWhere<K>(map.first, map.second, mapWhere.getValue(i)));
        }
        return result;
    }

    // группируем по keyEqual, or'им StatKeys и Where
    private <K> Collection<GroupStatWhere<K>> groupAll(boolean noWhere, QuickMap<KeyEqual, Where> keyEquals, Collection<GroupStatWhere<K>>... statJoinsList) {
        Collection<GroupStatWhere<K>> result = new ArrayList<GroupStatWhere<K>>();
        MapWhere<KeyEqual> mapWhere = new MapWhere<KeyEqual>(); MapStatKeys<KeyEqual, K> mapStats = new MapStatKeys<KeyEqual, K>();
        for(Collection<GroupStatWhere<K>> statJoins : statJoinsList)
            for(GroupStatWhere<K> statJoin : statJoins) {
                mapWhere.add(statJoin.keyEqual, noWhere || keyEquals!=null ? Where.TRUE : statJoin.where); mapStats.add(statJoin.keyEqual, statJoin.stats);
            }
        for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
            KeyEqual keys = mapWhere.getKey(i);
            result.add(new GroupStatWhere<K>(keys, mapStats.get(keys), keyEquals!=null ? keyEquals.get(keys) : mapWhere.getValue(i)));
        }
        return result;

    }

    public <K> Collection<GroupStatWhere<K>> group(Collection<GroupStatWhere<K>> statJoins, boolean noWhere, QuickMap<KeyEqual, Where> keyEquals) { // statJoins - в NONE группировке
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
    
    public <K> Collection<GroupStatWhere<K>> merge(Collection<GroupStatWhere<K>> joins1, Collection<GroupStatWhere<K>> joins2, boolean noWhere) { // joins1, joins2 в this группировке
        switch(this) {
            case NONE:
                return BaseUtils.merge(joins1, joins2);
            case STAT:
                return groupStat(noWhere, joins1, joins2);
            case ALL:
                return groupAll(noWhere, null, joins1, joins2);
        }
        throw new RuntimeException("should not be");
    }

    public <K> Collection<GroupStatWhere<K>> merge(Collection<GroupStatWhere<K>> joins1, GroupStatWhere<K> join) { // joins1 в this группировке
        // для оптимизации сделаем for'ом
        Collection<GroupStatWhere<K>> result = new ArrayList<GroupStatWhere<K>>(joins1);
        switch(this) {
            case NONE:
                break;
            case STAT: {
                for(Iterator<GroupStatWhere<K>> i=result.iterator();i.hasNext();) {
                    GroupStatWhere<K> where = i.next();
                    if(where.keyEqual.equals(join.keyEqual) && where.stats.equals(join.stats)) {
                        i.remove();
                        join = new GroupStatWhere<K>(join.keyEqual, join.stats, join.where.or(where.where));
                        break;
                    }
                }
                break;
            } case ALL:
                for(Iterator<GroupStatWhere<K>> i=result.iterator();i.hasNext();) {
                    GroupStatWhere<K> where = i.next();
                    if(where.keyEqual.equals(join.keyEqual)) {
                        i.remove();
                        join = new GroupStatWhere<K>(join.keyEqual, join.stats.or(where.stats), join.where.or(where.where));
                        break;
                    }
                }
                break;
            default:
                throw new RuntimeException("should not be");
        }
        result.add(join);
        return result;
    }
}
