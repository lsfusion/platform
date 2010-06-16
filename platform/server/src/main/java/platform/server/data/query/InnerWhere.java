package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.Lazy;
import platform.server.caches.hash.HashCodeContext;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.Table;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

// цель механизма InnerJoins, чтобы не было висячих ключей, из этого и следует InnerWhere
@Immutable
public class InnerWhere implements DNFWheres.Interface<InnerWhere> {

    private final JoinSet joins;
    private final Set<KeyExpr> keyObjects; // если есть IsClassWhere, убирает висячий ключ

    private final BaseClass baseClass; // дебилизм конечно, но не понятно откуда его тащить

    public JoinSet getJoins() { // нужно чтобы докинуть ObjectTable'ы, хотя потом может и изменим
        InnerJoin[] keyJoins = new InnerJoin[keyObjects.size()]; int joinNum = 0;
        for(KeyExpr keyObject : keyObjects) // всегда Table.Join так как не Session
            keyJoins[joinNum++] = (Table.Join) baseClass.table.joinAnd(Collections.singletonMap(baseClass.table.key,keyObject));
        return joins.and(new JoinSet(keyJoins));
    }

    public final Map<KeyExpr, BaseExpr> keyExprs;

    public InnerWhere() {
        joins = new JoinSet();
        keyExprs = new HashMap<KeyExpr, BaseExpr>();
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public InnerWhere(InnerJoin where) {
        joins = new JoinSet(where);
        keyExprs = new HashMap<KeyExpr, BaseExpr>();
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public InnerWhere(KeyExpr expr, BaseClass baseClass) {
        joins = new JoinSet();
        keyExprs = new HashMap<KeyExpr, BaseExpr>();
        keyObjects = Collections.singleton(expr);
        this.baseClass = baseClass;
    }

    public InnerWhere(KeyExpr key, BaseExpr expr) {
        joins = new JoinSet();
        assert !expr.hasKey(key);
        keyExprs = Collections.singletonMap(key, expr);
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public boolean means(InnerWhere where, ClassExprWhere classWhere) { // classWhere именно this а не параметра
//      здесь assert что все ключи должны быть
        if(!(BaseUtils.isSubMap(where.keyExprs,keyExprs) && joins.means(where.joins)))
            return false;

        ClassExprWhere keyClasses = ClassExprWhere.TRUE;
        for(KeyExpr keyObject : where.keyObjects)
            if(!keyObjects.contains(keyObject))
                keyClasses = keyClasses.and(new ClassExprWhere(keyObject, where.baseClass.getUpSet()));
        return classWhere.means(keyClasses);
    }

    public InnerWhere(JoinSet joins, Map<KeyExpr, BaseExpr> keyExprs, Set<KeyExpr> keyObjects, BaseClass baseClass) {
        this.joins = joins;
        this.keyExprs = keyExprs;
        this.keyObjects = keyObjects;
        this.baseClass = baseClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof InnerWhere && joins.equals(((InnerWhere) o).joins) && keyExprs.equals(((InnerWhere) o).keyExprs) && keyObjects.equals(((InnerWhere) o).keyObjects);
    }

    @Override
    public int hashCode() {
        return hashContext(HashCodeContext.instance);
    }

    private static void and(Set<KeyExpr> result, Set<KeyExpr> and, JoinSet exclJoins, Set<KeyExpr> exclExprs) {
        VariableExprSet exclFollows = exclJoins.getJoinFollows();
        for(KeyExpr expr : and)
            if(!(exclFollows.contains(expr) || exclExprs.contains(expr)))
                result.add(expr);
    }

    public InnerWhere and(InnerWhere where) {
        JoinSet andJoins = joins.and(where.joins);
        Map<KeyExpr, BaseExpr> andKeyExprs = BaseUtils.merge(keyExprs, where.keyExprs); // даже если совпадают ничего страшного, все равно зафиксировано в InnerJoins - Where, а ключ висячий не появляется
        Set<KeyExpr> andKeyObjects = new HashSet<KeyExpr>();
        and(andKeyObjects, keyObjects, where.joins, where.keyExprs.keySet());
        and(andKeyObjects, where.keyObjects, joins, keyExprs.keySet());
        return new InnerWhere(andJoins, andKeyExprs, andKeyObjects, BaseUtils.nvl(baseClass,where.baseClass));
    }

    public boolean isFalse() {
        return false;
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyExprs.entrySet())
            hash += keyValue.getKey().hashContext(hashContext) ^ keyValue.getValue().hashContext(hashContext);
        hash = hash * 31;
        for(KeyExpr keyObject : keyObjects)
            hash += keyObject.hashContext(hashContext);
        return hash * 31 + joins.hashContext(hashContext);
    }

    public InnerWhere translate(MapTranslate translator) {
        Map<KeyExpr,BaseExpr> transKeyExprs = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> keyValue : keyExprs.entrySet())
            transKeyExprs.put(keyValue.getKey().translate(translator),keyValue.getValue().translate(translator));
        return new InnerWhere(joins.translate(translator),transKeyExprs,translator.translateKeys(keyObjects),baseClass);
    }
}
