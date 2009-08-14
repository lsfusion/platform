package platform.server.data.classes.where;

import platform.server.data.classes.ValueClass;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.GroupExpr;
import platform.server.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassWhere<K> extends AbstractClassWhere<K, ClassWhere<K>> {

    public ClassWhere() {
        this(false);
    }

    public ClassWhere(boolean isTrue) {
        super(isTrue);
    }

    public ClassWhere(And<K> where) {
        super(where);
    }

    public static <K> ClassWhere<K> TRUE() {
        return new ClassWhere<K>(true);
    }
    public static <K> ClassWhere<K> STATIC(boolean isTrue) {
        return new ClassWhere<K>(isTrue);
    }

    private ClassWhere(And<K>[] iWheres) {
        super(iWheres);
    }
    protected ClassWhere<K> createThis(And<K>[] iWheres) {
        return new ClassWhere<K>(iWheres);
    }

    public ClassWhere(K key, AndClassSet classes) {
        super(key, classes);
    }

    private static <K> Map<K,AndClassSet> initUpClassSets(Map<K, ValueClass> map) {
        Map<K,AndClassSet> result = new HashMap<K,AndClassSet>();
        for(Map.Entry<K, ValueClass> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().getUpSet());
        return result;
    }
    public ClassWhere(Map<K, ValueClass> mapClasses,boolean up) {
        super(initUpClassSets(mapClasses));
        assert up;
    }



    public ClassWhere(Map<K, ? extends AndClassSet> mapSets) {
        super(mapSets);
    }

    public Map<K, ValueClass> getCommonParent(Collection<K> keys) {

        assert !isFalse();

        Map<K, ValueClass> result = new HashMap<K, ValueClass>();
        for(K key : keys) {
            OrClassSet orSet = wheres[0].get(key).getOr();
            for(int i=1;i<wheres.length;i++)
                orSet = orSet.or(wheres[i].get(key).getOr());
            result.put(key,orSet.getCommonClass());
        }
        return result;
    }

    public ClassExprWhere map(Map<K, AndExpr> map) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<K> andWhere : wheres) {
            ClassExprWhere joinWhere = ClassExprWhere.TRUE;
            for(Map.Entry<K, AndExpr> joinExpr : map.entrySet())
                joinWhere = joinWhere.and(joinExpr.getValue().getClassWhere(andWhere.get(joinExpr.getKey())));
            result = result.or(joinWhere);
        }
        return result;
    }

    public <V> ClassWhere(ClassWhere<V> classes, Map<V, K> map) {
        super(classes, map);
    }
}

