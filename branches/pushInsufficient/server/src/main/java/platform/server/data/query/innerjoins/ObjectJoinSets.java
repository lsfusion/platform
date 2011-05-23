package platform.server.data.query.innerjoins;

import platform.server.classes.BaseClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.JoinSet;
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
    public Map<ObjectJoinSet, Where> compileMeans() {

        Map<ObjectJoinSet, Where> result = new HashMap<ObjectJoinSet, Where>();
        for(int i=0;i<size;i++) {
            ObjectJoinSet objectJoin = getKey(i);
            Where where = getValue(i);

            if(Settings.instance.isCompileMeans()) {
                boolean found = false;
                // ищем кого-нибудь кого он means
                for(Map.Entry<ObjectJoinSet,Where> resultJoin : result.entrySet())
                    if(objectJoin.means(resultJoin.getKey(), where.getClassWhere())) {
                        resultJoin.setValue(resultJoin.getValue().or(where)); //.and(whereJoin.mean.followFalse(resultJoin.mean.not())
                        found = true;
                    }
                if(!found) {
                    // ищем все кто его means и удаляем
                    for(Iterator<Map.Entry<ObjectJoinSet,Where>> it = result.entrySet().iterator();it.hasNext();) {
                        Map.Entry<ObjectJoinSet,Where> resultJoin = it.next();
                        if(resultJoin.getKey().means(objectJoin, resultJoin.getValue().getClassWhere())) {
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

    public MapWhere<Set<KeyExpr>> compileEnough(Set<KeyExpr> keys) {
        MapWhere<Set<KeyExpr>> mapWhere = new MapWhere<Set<KeyExpr>>();

        Collection<Where> fullKeys = new ArrayList<Where>(); // чисто для оптимизации
        for(int i=0;i<size;i++) {
            ObjectJoinSet objectJoin = getKey(i);
            Where where = getValue(i);

            Set<KeyExpr> insufKeys = objectJoin.getJoins().insufficientKeys(keys);
            if(fullKeys!=null && insufKeys.size() == 0)
                fullKeys.add(where);
            else {
                if(fullKeys!=null) {
                    for(Where fullKey : fullKeys)
                        mapWhere.add(new HashSet<KeyExpr>(), fullKey);
                    fullKeys = null;
                }
                mapWhere.add(insufKeys, where);
            }
        }

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
