package platform.server.data.expr;

import platform.base.QuickMap;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.classes.ClassExprWhere;

public abstract class VariableClassExpr extends SingleClassExpr {

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    protected abstract VariableClassExpr translate(MapTranslate translator);
    public VariableClassExpr translateOuter(MapTranslate translator) {
        return (VariableClassExpr) aspectTranslate(translator);
    }

    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return and.getPartial(this);
    }
    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        boolean added = and.add(this, add);
        assert added;
        return true;
    }
}
