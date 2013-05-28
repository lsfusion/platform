package platform.server.data.expr;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.classes.ClassExprWhere;

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
