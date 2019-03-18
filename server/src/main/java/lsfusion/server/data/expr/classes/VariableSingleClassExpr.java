package lsfusion.server.data.expr.classes;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public abstract class VariableSingleClassExpr extends SingleClassExpr {

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    protected abstract VariableSingleClassExpr translate(MapTranslate translator);
    public VariableSingleClassExpr translateOuter(MapTranslate translator) {
        return (VariableSingleClassExpr) aspectTranslate(translator);
    }

    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return and.getPartial(this);
    }
    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        boolean added = and.add(this, add);
        assert added;
        return true;
    }
}
