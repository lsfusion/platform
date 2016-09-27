package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.ExprType;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaImpl;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
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

    public FormulaImplProperty(LocalizedString caption, int intCount, FormulaJoinImpl formula) {
        super(caption, getInterfaces(intCount));

        this.formula = formula;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return FormulaExpr.create(formula, getOrderInterfaces().mapList(joinImplement));
    }
    
    public static <T extends PropertyInterface> ExClassSet inferValueClass(final ImOrderSet<T> orderInterfaces, FormulaImpl formula, final ImMap<T, ExClassSet> inferred) {
        final ImList<ExClassSet> orderTypes = orderInterfaces.mapList(inferred);
        return ExClassSet.toExType(formula.getType(new ExprType() {

            public int getExprCount() {
                return orderInterfaces.size();
            }

            public Type getType(int i) {
                return ExClassSet.fromExType(orderTypes.get(i));
            }
        }));
    }
    
    public lsfusion.server.logics.property.infer.ExClassSet calcInferValueClass(final ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return inferValueClass(getOrderInterfaces(), formula, inferred);
    }
}
