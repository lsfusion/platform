package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class CastProperty extends FormulaProperty<CastProperty.Interface> {

    private DataClass castClass;

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public CastProperty(String sID, String caption, DataClass castClass) {
        super(sID, caption, SetFact.singletonOrder(new Interface(0)));

        this.castClass = castClass;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr expr = joinImplement.get(getOrderInterfaces().get(0));
        return FormulaExpr.createCast(expr, castClass);
    }
}
