package platform.server.data.classes.where;

import platform.server.data.classes.ValueClass;
import platform.server.data.classes.DataClass;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.MapKeysInterface;
import platform.server.where.Where;

import java.util.*;

public class ClassWhere<K> extends AbstractClassWhere<K,AndClassWhere<K>,ClassWhere<K>> {

    private ClassWhere(AndClassWhere<K>[] iWheres) {
        super(iWheres);
    }
    protected ClassWhere<K> createThis(AndClassWhere<K>[] iWheres) {
        return new ClassWhere<K>(iWheres);
    }

    protected ClassWhere<K> getThis() {
        return this;
    }

    public ClassWhere() {
    }

    public ClassWhere(AndClassWhere<K> andWhere) {
        super(andWhere);
    }

    public ClassWhere(K key, ClassSet classes) {
        super(new AndClassWhere<K>(key, classes));
    }
    private static <K> AndClassWhere<K> initClassSets(Map<K, ValueClass> map) {
        AndClassWhere<K> result = new AndClassWhere<K>();
        for(Map.Entry<K, ValueClass> entry : map.entrySet())
            result.add(entry.getKey(),entry.getValue().getUpSet());
        return result;
    }
    public ClassWhere(Map<K, ValueClass> mapClasses) {
        this(initClassSets(mapClasses));
    }

    public static <K> ClassWhere<K> get(Map<K, AndExpr> map, Where exprWhere) {
        ClassWhere<K> transWhere = new ClassWhere<K>();
        for(AndClassExprWhere andWhere : exprWhere.getClassWhere().wheres)
            transWhere = transWhere.or(andWhere.get(map));
        return transWhere;
    }

    public Set<K> getKeys() { // в общем то для partial 
        Set<K> result = new HashSet<K>();
        for(AndClassWhere<K> andWhere : wheres)
            andWhere.fillKeys(result);
        return result;
    }

    protected AndClassWhere<K>[] newArray(int size) {
        return new AndClassWhere[size];
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
        for(AndClassWhere<K> andWhere : wheres) {
            ClassExprWhere joinWhere = ClassExprWhere.TRUE;
            for(Map.Entry<K, AndExpr> joinExpr : map.entrySet())
                joinWhere = joinWhere.and(joinExpr.getValue().getClassWhere(andWhere.get(joinExpr.getKey())));
            result = result.or(joinWhere);
        }

        Where where = Where.TRUE; // теоретически можно под map перенести
        for(AndExpr expr : map.values())
            where = where.and(expr.getWhere());
        return result.and(where.getClassWhere());
    }

    public <V> ClassWhere<V> mapKeys(Map<K,V> map) {
        AndClassWhere<V>[] mapWheres = new AndClassWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            mapWheres[i] = wheres[i].mapKeys(map);
        return new ClassWhere<V>(mapWheres);
    }
}

