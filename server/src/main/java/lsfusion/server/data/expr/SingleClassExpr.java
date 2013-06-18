package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrClassSet;
import lsfusion.server.data.expr.where.extra.IsClassWhere;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.property.ClassField;

public abstract class SingleClassExpr extends VariableClassExpr {

    protected abstract SingleClassExpr translate(MapTranslate translator);
    public SingleClassExpr translateOuter(MapTranslate translator) {
        return (SingleClassExpr) aspectTranslate(translator);
    }

    private boolean isTrueWhere() {
        return this instanceof ParamExpr || this instanceof CurrentEnvironmentExpr;
    }

    public Expr classExpr(ImSet<ClassField> classes, IsClassType type) {
/*        ConcreteObjectClass singleClass;
        if(!isTrueWhere() && ((singleClass = ((OrObjectClassSet)getSet().and(classes.getOr())).getSingleClass(classes.getBaseClass()))!=null))
            return singleClass.getClassObject().getStaticExpr().and(getWhere());*/

        return IsClassExpr.create(this, classes, type);
    }

    private OrClassSet getSet() {
        assert !isTrueWhere();
        OrClassSet result = null;
        for(ImMap<VariableSingleClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds()) {
            OrClassSet classSet = getAndClassSet(where).getOr();
            if(result==null)
                result = classSet;
            else
                result = result.or(classSet);
        }
        assert result!=null;
        return result;
    }

    private boolean intersect(AndClassSet set) {
        if(isTrueWhere())
            return !set.isEmpty();
        else {
            for(ImMap<VariableSingleClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds())
                if(!getAndClassSet(where).and(set).isEmpty()) return true; // тут наверное тоже надо getAndClassSet на не null проверять для не полных случаев
            return false;
        }
    }

    public Where isClass(ValueClassSet set, boolean inconsistent) {
        if(!inconsistent) {
            // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
            if(!intersect(set)) // если не пересекается то false
                return Where.FALSE;
            if(!isTrueWhere())
                if(set.getOr().containsAll(getSet(), true)) // если set содержит все элементы, то достаточно просто что не null (implicit cast'ы подходят)
                    return getWhere();
        }
        return IsClassWhere.create(this, set, inconsistent);
    }
}
