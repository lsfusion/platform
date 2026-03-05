package lsfusion.server.data.expr.classes;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.CurrentEnvironmentExpr;
import lsfusion.server.data.expr.where.classes.IsClassWhere;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public abstract class SingleClassExpr extends VariableClassExpr {

    protected abstract SingleClassExpr translate(MapTranslate translator);
    public SingleClassExpr translateOuter(MapTranslate translator) {
        return (SingleClassExpr) aspectTranslate(translator);
    }

    private boolean isTrueWhere() {
        return this instanceof ParamExpr || this instanceof CurrentEnvironmentExpr;
    }

    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
/*        ConcreteObjectClass singleClass;
        if(!isTrueWhere() && ((singleClass = ((OrObjectClassSet)getSet().and(classes.getOr())).getSingleClass(classes.getBaseClass()))!=null))
            return singleClass.getClassObject().getStaticExpr().and(getWhere());*/

        return IsClassExpr.create(this, classes, type);
    }

    private boolean intersect(AndClassSet set) {
        if(isTrueWhere())
            return !set.isEmpty();
        else {
            for(ImMap<VariableSingleClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds()) {
                AndClassSet andClassSet = getAndClassSet(where);
                if (andClassSet == null) {
                    assert false;
                    return !set.isEmpty();
                }
                if(!andClassSet.and(set).isEmpty()) return true; // тут наверное тоже надо getAndClassSet на не null проверять для не полных случаев
            }
            return false;
        }
    }

    public Where isClass(ValueClassSet set, IsClassType type) {
        if(!type.isInconsistent()) {
            // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
            if(!intersect(set)) // если не пересекается то false
                return Where.FALSE();
            if(!isTrueWhere()) {
                AndClassSet andClassSet = getWhere().getClassWhere().getAndClassSet(this);
                if(andClassSet != null && set.containsAll(andClassSet, true)) // если set содержит все элементы, то достаточно просто что не null (implicit cast'ы подходят)
                    return getWhere();
            }
        }
        return IsClassWhere.create(this, set, type);
    }
}
