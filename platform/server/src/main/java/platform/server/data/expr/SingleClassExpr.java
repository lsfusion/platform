package platform.server.data.expr;

import platform.server.data.translator.DirectTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.where.IsClassWhere;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.base.QuickMap;

public abstract class SingleClassExpr extends BaseExpr {

    public abstract SingleClassExpr translateDirect(DirectTranslator translator);

    private Expr classExpr;
    public Expr classExpr(BaseClass baseClass) {
        if(classExpr==null) {
            ConcreteObjectClass singleClass;
            if(!(this instanceof KeyExpr) && ((singleClass = ((OrObjectClassSet)getSet()).getSingleClass(baseClass))!=null))
                classExpr = singleClass.getIDExpr().and(getWhere());
            else
                classExpr = new IsClassExpr(this,baseClass);
        }
        return classExpr;
    }

    private OrClassSet getSet() {
        assert !(this instanceof KeyExpr);
        OrClassSet result = null;
        for(QuickMap<VariableClassExpr,AndClassSet> where : getWhere().getClassWhere().getAnds()) {
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
        if(this instanceof KeyExpr)
            return !set.isEmpty();
        else {
            for(QuickMap<VariableClassExpr, AndClassSet> where : getWhere().getClassWhere().getAnds())
                if(!getAndClassSet(where).and(set).isEmpty()) return true;
            return false;
        }
    }

    public Where isClass(AndClassSet set) {
        // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
        if(!intersect(set)) // если не пересекается то false
            return Where.FALSE;
        if(!(this instanceof KeyExpr))
            if(set.getOr().containsAll(getSet())) // если set содержит все элементы, то достаточно просто что не null
                return getWhere();
        return new IsClassWhere(this,set);
    }
}
