package platform.server.data.query.innerjoins;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.data.Table;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.JoinSet;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ObjectJoinSet implements DNFWheres.Interface<ObjectJoinSet> {

    private final JoinSet joins;
    private final Set<KeyExpr> keyObjects; // если есть IsClassWhere, убирает висячий ключ

    private final BaseClass baseClass; // дебилизм конечно, но не понятно откуда его тащить

    public JoinSet getJoins() { // нужно чтобы докинуть ObjectTable'ы, хотя потом может и изменим
        InnerJoin[] keyJoins = new InnerJoin[keyObjects.size()]; int joinNum = 0;
        for(KeyExpr keyObject : keyObjects) // всегда Table.Join так как не Session
            keyJoins[joinNum++] = (Table.Join) baseClass.table.joinAnd(Collections.singletonMap(baseClass.table.key,keyObject));
        return joins.and(new JoinSet(keyJoins));
    }

    public ObjectJoinSet() {
        joins = new JoinSet();
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public ObjectJoinSet(InnerJoin where) {
        joins = new JoinSet(where);
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public ObjectJoinSet(KeyExpr expr, BaseClass baseClass) {
        joins = new JoinSet();
        keyObjects = Collections.singleton(expr);
        this.baseClass = baseClass;
    }

    public ObjectJoinSet(KeyExpr key, BaseExpr expr) {
        joins = new JoinSet();
        assert !expr.hasKey(key);
        keyObjects = new HashSet<KeyExpr>();
        baseClass = null;
    }

    public boolean means(ObjectJoinSet where, ClassExprWhere classWhere) { // classWhere именно this а не параметра
//      здесь assert что все ключи должны быть
        if(!joins.means(where.joins))
            return false;

        ClassExprWhere keyClasses = ClassExprWhere.TRUE;
        for(KeyExpr keyObject : where.keyObjects)
            if(!keyObjects.contains(keyObject))
                keyClasses = keyClasses.and(new ClassExprWhere(keyObject, where.baseClass.getUpSet()));
        return classWhere.means(keyClasses);
    }

    public ObjectJoinSet(JoinSet joins, Set<KeyExpr> keyObjects, BaseClass baseClass) {
        this.joins = joins;
        this.keyObjects = keyObjects;
        this.baseClass = baseClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ObjectJoinSet && joins.equals(((ObjectJoinSet) o).joins) && keyObjects.equals(((ObjectJoinSet) o).keyObjects);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for(KeyExpr keyObject : keyObjects)
            hash += keyObject.hashCode();
        return hash * 31 + joins.hashCode();
    }

    private static void and(Set<KeyExpr> result, Set<KeyExpr> and, JoinSet exclJoins) {
        VariableExprSet exclFollows = exclJoins.getJoinFollows();
        for(KeyExpr expr : and)
            if(!(exclFollows.contains(expr)))
                result.add(expr);
    }

    public ObjectJoinSet and(ObjectJoinSet where) {
        JoinSet andJoins = joins.and(where.joins);
        Set<KeyExpr> andKeyObjects = new HashSet<KeyExpr>();
        and(andKeyObjects, keyObjects, where.joins);
        and(andKeyObjects, where.keyObjects, joins);
        return new ObjectJoinSet(andJoins, andKeyObjects, BaseUtils.nvl(baseClass,where.baseClass));
    }

    public boolean isFalse() {
        return false;
    }

    public ObjectJoinSet translateOuter(MapTranslate translator) {
        return new ObjectJoinSet(joins.translateOuter(translator),translator.translateKeys(keyObjects),baseClass);
    }
}
