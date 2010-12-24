package platform.server.session;

import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.classes.ValueClass;
import platform.server.classes.BaseClass;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.util.Map;

public class UsedSimpleChanges implements ExprChanges {
    private final SimpleChanges changes;

    UsedSimpleChanges(SimpleChanges changes) {
        this.changes = changes;
    }

    // предполагается что все до getUsedChanges вызывается только для IncrementUpdate в FormInstance при проходе через MaxChange и Cycle
    public Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        return expr.isClass(isClass.getUpSet());
    }

    public Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres) {
        return expr.classExpr(baseClass);
    }

    public Join<String> getDataChange(DataProperty property, Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        return null;
    }

    public Where getRemoveWhere(ValueClass valueClass, Expr expr) {
        return Where.FALSE;
    }

    public SimpleChanges getUsedChanges() {
        return changes;
    }
}
