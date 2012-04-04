package platform.server.data.where.classes;

import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Field;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.Stat;
import platform.server.data.where.Where;

import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class ClassWhere<K> extends AbstractClassWhere<K, ClassWhere<K>> {

    private boolean compatible(ClassWhere<K> where) {
        return where.isFalse() || isFalse() || wheres[0].compatible(where.wheres[0]);
    }
    // в некоторых случаях как при проверке интерфейсов, классы могут быть не compatible и тогда нарушится инвариант с OrClassSet -
    // что это либо OrObjectClassSet или DataClass поэтому пока так
    public ClassWhere<K> andCompatible(ClassWhere<K> where) {
        if(compatible(where))
            return and(where);
        else
            return STATIC(false);
    }

    public boolean meansCompatible(ClassWhere<K> where) {
        return compatible(where) && means(where);
    }

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
        return STATIC(true);
    }
    public static <K> ClassWhere<K> STATIC(boolean isTrue) {
        return new ClassWhere<K>(isTrue);
    }

    public static <M,K> Map<M,ClassWhere<K>> STATIC(Collection<M> keys, boolean isTrue) {
        Map<M, ClassWhere<K>> result = new HashMap<M, ClassWhere<K>>();
        for(M key : keys)
            result.put(key, ClassWhere.<K>STATIC(isTrue));
        return result;
    }

    private ClassWhere(And<K>[] iWheres) {
        super(iWheres);
    }
    protected ClassWhere<K> createThis(And<K>[] wheres) {
        return new ClassWhere<K>(wheres);
    }

    public ClassWhere(K key, AndClassSet classes) {
        super(key, classes);
    }

    public ClassWhere(Map<K, ValueClass> mapClasses,boolean up) {
        super(mapClasses, up);
    }



    public ClassWhere(Map<K, ? extends AndClassSet> mapSets) {
        super(mapSets);
    }

    public ClassExprWhere map(Map<K, BaseExpr> map) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<K> andWhere : wheres) {
            ClassExprWhere joinWhere = ClassExprWhere.TRUE;
            for(Map.Entry<K, BaseExpr> joinExpr : map.entrySet())
                joinWhere = joinWhere.and(joinExpr.getValue().getClassWhere(andWhere.get(joinExpr.getKey())));
            result = result.or(joinWhere);
        }
        return result;
    }

    public <V> ClassWhere(ClassWhere<V> classes, Map<V, K> map) {
        super(classes, map);
    }

    public Stat getTypeStat(K key) {
        return wheres[0].get(key).getTypeStat();
    }

    // аналогичный метод в ClassExprWhere
    public ClassWhere<K> remove(Collection<? extends K> keys) {
        ClassWhere<K> result = ClassWhere.STATIC(false);
        for(And<K> andWhere : wheres)
            result = result.or(new ClassWhere<K>(andWhere.remove(keys)));
        return result;
    }

    public Where getWhere(Map<K, ? extends Expr> mapExprs) {
        Where result = Where.FALSE;
        for(And<K> andWhere : wheres)
            result = result.or(andWhere.getWhere(mapExprs));
        return result;

    }
}

