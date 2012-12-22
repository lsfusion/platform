package platform.server.data.expr;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.expr.where.extra.IsClassWhere;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

public abstract class SingleClassExpr extends BaseExpr {

    protected abstract SingleClassExpr translate(MapTranslate translator);
    public SingleClassExpr translateOuter(MapTranslate translator) {
        return (SingleClassExpr) aspectTranslate(translator);
    }

    private boolean isTrueWhere() {
        return this instanceof KeyExpr || this instanceof CurrentEnvironmentExpr;
    }

    private Expr classExpr;
    public Expr classExpr(BaseClass baseClass) {
        if(classExpr==null) {
            ConcreteObjectClass singleClass;
            if(!isTrueWhere() && ((singleClass = ((OrObjectClassSet)getSet()).getSingleClass(baseClass))!=null))
                classExpr = singleClass.getClassObject().getStaticExpr().and(getWhere());
            else
                classExpr = new IsClassExpr(this,baseClass);
        }
        return classExpr;
    }

    private OrClassSet getSet() {
        assert !isTrueWhere();
        OrClassSet result = null;
        for(ImMap<VariableClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds()) {
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
            for(ImMap<VariableClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds())
                if(!getAndClassSet(where).and(set).isEmpty()) return true; // тут наверное тоже надо getAndClassSet на не null проверять для не полных случаев
            return false;
        }
    }

    public Where isClass(AndClassSet set) {
        // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
        if(!intersect(set)) // если не пересекается то false
            return Where.FALSE;
        if(!isTrueWhere())
            if(set.getOr().containsAll(getSet())) // если set содержит все элементы, то достаточно просто что не null
                return getWhere();
        return new IsClassWhere(this,set);
    }
}
