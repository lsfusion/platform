package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class FormulaImplProperty extends FormulaProperty<FormulaImplProperty.Interface> {

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

    private final FormulaJoinImpl formula;

    public FormulaImplProperty(String caption, int intCount, FormulaJoinImpl formula) {
        super(caption, getInterfaces(intCount));

        this.formula = formula;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return FormulaExpr.create(formula, getOrderInterfaces().mapList(joinImplement));
    }
}
