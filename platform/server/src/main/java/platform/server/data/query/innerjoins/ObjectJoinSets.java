package platform.server.data.query.innerjoins;

import platform.server.classes.BaseClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.StatKeys;
import platform.server.data.where.MapWhere;
import platform.server.data.query.InnerJoin;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;
import platform.server.Settings;

import java.util.*;

// используется только в groupObjectJoinSets, по сути protected класс
public class ObjectJoinSets extends DNFWheres<ObjectJoinSet, Where, ObjectJoinSets> {

    protected Where andValue(Where prevValue, Where newValue) {
        return prevValue.and(newValue);
    }
    protected Where addValue(Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected ObjectJoinSets createThis() {
        return new ObjectJoinSets();
    }

    // компилирует запрос на выполнение группируя means'ы, отдельно means по-любому нельзя так как JoinSelect спрячется за not,
    // который потом может уйти в followFalse, а JoinSelect так и останется в Where торчать и не хватит ключа
    // а так у нас есть гарантия что ключей хватит
    public Map<ObjectJoinSet, Where> compileMeans(Set<KeyExpr> keys) {

        Map<ObjectJoinSet, Where> result = new HashMap<ObjectJoinSet, Where>();
        for(int i=0;i<size;i++) {
            ObjectJoinSet objectJoin = getKey(i);
            Where where = getValue(i);

            if(Settings.instance.isCompileMeans()) {
                boolean found = false;
                // ищем кого-нибудь кого он means
                for(Map.Entry<ObjectJoinSet,Where> resultJoin : result.entrySet())
                    if(objectJoin.means(resultJoin.getKey(), where.getClassWhere(), keys)) {
                        resultJoin.setValue(resultJoin.getValue().or(where)); //.and(whereJoin.mean.followFalse(resultJoin.mean.not())
                        found = true;
                    }
                if(!found) {
                    // ищем все кто его means и удаляем
                    for(Iterator<Map.Entry<ObjectJoinSet,Where>> it = result.entrySet().iterator();it.hasNext();) {
                        Map.Entry<ObjectJoinSet,Where> resultJoin = it.next();
                        if(resultJoin.getKey().means(objectJoin, resultJoin.getValue().getClassWhere(), keys)) {
                            where = where.or(resultJoin.getValue());
                            it.remove();
                        }
                    }
                    result.put(objectJoin, where);
                }
            } else
                result.put(objectJoin, where);
        }

        return result;
    }

    public MapWhere<StatKeys<KeyExpr>> compileStats(Set<KeyExpr> keys) {
        MapWhere<StatKeys<KeyExpr>> mapWhere = new MapWhere<StatKeys<KeyExpr>>();
        for(int i=0;i<size;i++)
            mapWhere.add(getKey(i).getStatKeys(keys), getValue(i));
        return mapWhere;
    }

    public ObjectJoinSets() {
    }

    private ObjectJoinSets(ObjectJoinSet inner, Where where) {
        super(inner, where);
    }

    public ObjectJoinSets(Where where) {
        this(new ObjectJoinSet(),where);
    }

    public ObjectJoinSets(KeyExpr expr, BaseClass baseClass, Where where) {
        this(new ObjectJoinSet(expr, baseClass),where);
    }

    public ObjectJoinSets(InnerJoin join,Where where) {
        this(new ObjectJoinSet(join),where);
    }
}
