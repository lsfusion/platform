package platform.server.session;

import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;

import java.util.Map;

// MUTABLE
public interface ExprChanges {

    final static ExprChanges EMPTY = new ExprChanges() {
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
            return SimpleChanges.EMPTY;
        }
    };

    Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres);

    Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres);

    Join<String> getDataChange(DataProperty property, Map<ClassPropertyInterface, ? extends Expr> joinImplement);
    
    Where getRemoveWhere(ValueClass valueClass, Expr expr);

    // IMMUTABLE
    SimpleChanges getUsedChanges();
}
