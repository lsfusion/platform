package platform.server.data.expr;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.DirectTranslator;
import platform.base.QuickMap;

public abstract class VariableClassExpr extends SingleClassExpr {

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    public abstract VariableClassExpr translateDirect(DirectTranslator translator);

    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return and.getPartial(this);
    }
    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        boolean added = and.add(this, add);
        assert added;
        return true;
    }
}
