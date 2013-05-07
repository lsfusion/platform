package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.data.expr.Expr;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class SumProperty extends FormulaProperty<SumProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }
        });
    }

    public SumProperty(String sID, String caption) {
        super(sID, caption, getInterfaces(2));

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr expr1 = joinImplement.get(getOrderInterfaces().get(0));
        Expr expr2 = joinImplement.get(getOrderInterfaces().get(1));
        return FormulaExpr.createSum(expr1, expr2);
    }
}
