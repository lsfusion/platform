package platform.server.data.classes.where;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.StaticClassExpr;
import platform.server.data.query.exprs.VariableClassExpr;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.where.Where;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

public class AndClassExprWhere extends AbstractAndClassWhere<VariableClassExpr,AndClassExprWhere> {

    public AndClassExprWhere() {
    }

    public AndClassExprWhere(VariableClassExpr key, ClassSet classes) {
        super(key, classes);
    }

    public <K> ClassWhere<K> get(Map<K, AndExpr> map) {
        AndClassWhere<K> andTrans = new AndClassWhere<K>();
        for(Map.Entry<K,AndExpr> mapEntry : map.entrySet()) {
            if(!andTrans.add(mapEntry.getKey(), mapEntry.getValue() instanceof StaticClassExpr?
                    ((StaticClassExpr) mapEntry.getValue()).getStaticClass():
                    get((VariableClassExpr) mapEntry.getValue())))
                return new ClassWhere<K>(); // возвращаем false
        }
        return new ClassWhere<K>(andTrans);
    }

    protected AndClassExprWhere getThis() {
        return this;
    }

    public AndClassExprWhere(AndClassExprWhere set) {
        super(set);
    }
    protected AndClassExprWhere copy() {
        return new AndClassExprWhere(this);
    }

    Collection<VariableClassExpr> keySet() {
        Collection<VariableClassExpr> result = new ArrayList<VariableClassExpr>();
        for(int i=0;i<size;i++)
            result.add((VariableClassExpr)table[indexes[i]]);
        return result;
    }
}
