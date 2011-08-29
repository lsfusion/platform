package platform.server.data.query.innerjoins;

import platform.base.Pair;
import platform.server.Settings;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.where.MapStatKeys;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;

import java.util.*;

public class GroupJoinsWhere extends GroupStatWhere<WhereJoins> {

    public final Map<WhereJoin, Where> upWheres;

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Map<WhereJoin, Where> upWheres, Where where) {
        super(keyEqual, joins, where);

        this.upWheres = upWheres;
    }

    public GroupJoinsWhere pack() { // upWheres особого смысла паковать нет, все равно
        return new GroupJoinsWhere(keyEqual, joins, upWheres, where.pack());
    }

    public static Collection<GroupJoinsWhere> pack(Collection<GroupJoinsWhere> whereJoins) {
        if(whereJoins.size()==1) // нет смысла упаковывать если один whereJoins
            return whereJoins;
        else {
            Collection<GroupJoinsWhere> result = new ArrayList<GroupJoinsWhere>();
            for(GroupJoinsWhere innerJoin : whereJoins)
                result.add(innerJoin.pack());
            return result;
        }
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups) {
        return joins.getStatKeys(groups, where, keyEqual);
    }

    public static <K extends BaseExpr> Collection<GroupStatWhere> groupStat(Collection<GroupJoinsWhere> innerJoins, Set<K> groups) {
        Collection<GroupStatWhere> result = new ArrayList<GroupStatWhere>();

        if (Settings.instance.isSplitGroupStatInnerJoins()) {
            MapWhere<Pair<KeyEqual, StatKeys<K>>> mapWhere = new MapWhere<Pair<KeyEqual, StatKeys<K>>>();
            for(GroupJoinsWhere innerJoin : innerJoins)
                mapWhere.add(new Pair<KeyEqual, StatKeys<K>>(innerJoin.keyEqual,
                        innerJoin.getStatKeys(groups)), innerJoin.where);
            for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
                Pair<KeyEqual, StatKeys<K>> map = mapWhere.getKey(i);
                result.add(new GroupStatWhere<GroupStatKeys<K>>(map.first, new GroupStatKeys<K>(map.second), mapWhere.getValue(i)));
            }
        } else {
            MapWhere<KeyEqual> mapWhere = new MapWhere<KeyEqual>(); MapStatKeys<KeyEqual, K> mapStats = new MapStatKeys<KeyEqual, K>();
            for(GroupJoinsWhere innerJoin : innerJoins) {
                mapWhere.add(innerJoin.keyEqual, innerJoin.where); mapStats.add(innerJoin.keyEqual, innerJoin.getStatKeys(groups));
            }
            for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
                KeyEqual keys = mapWhere.getKey(i);
                result.add(new GroupStatWhere<GroupStatKeys<K>>(keys, new GroupStatKeys<K>(mapStats.get(keys)), mapWhere.getValue(i)));
            }
        }
        return result;
    }
}
