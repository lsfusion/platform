package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.IntFunction;

public class FormulaJoinProperty extends FormulaProperty<FormulaJoinProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, Interface::new);
    }

    public final FormulaJoinImpl formula;

    public FormulaJoinProperty(DataClass valueClass, CustomFormulaSyntax formula, ImOrderSet<String> params, boolean valueNull) {
        this(valueClass, valueNull, formula, getInterfaces(params.size()), params);
    }
    private FormulaJoinProperty(DataClass valueClass, boolean valueNull, CustomFormulaSyntax formula, ImOrderSet<Interface> interfaces, ImOrderSet<String> params) {
        this(LocalizedString.create(formula.getDefaultSyntax()), interfaces, FormulaExpr.createJoinCustomFormulaImpl(formula, valueClass, valueNull, params));
    }

    public FormulaJoinProperty(LocalizedString caption, int intCount, FormulaJoinImpl formula) {
        this(caption, getInterfaces(intCount), formula);
    }
    public FormulaJoinProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, FormulaJoinImpl formula) {
        super(caption, interfaces);

        this.formula = formula;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return FormulaExpr.create(formula, getOrderInterfaces().mapList(joinImplement));
    }

    public static ExClassSet inferInterfaceClass(ExClassSet commonValue, FormulaImpl formula, int index) {
        return null; // maybe should be inferred from formula, but there are implicit casts there (however for arithmetic operations it can make sense)
    }

    public static ExClassSet inferValueClass(FormulaImpl formula, ImList<ExClassSet> orderTypes) {
        return ExClassSet.toExType(formula.getType(new ExprType() {

            public int getExprCount() {
                return orderTypes.size();
            }

            public Type getType(int i) {
                return ExClassSet.fromExType(orderTypes.get(i));
            }
        }));
    }

    @Override
    protected Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(getOrderInterfaces().mapOrderValues((IntFunction<ExClassSet>) index -> inferInterfaceClass(commonValue, formula, index)));
    }

    public ExClassSet calcInferValueClass(final ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return inferValueClass(formula, getOrderInterfaces().mapList(inferred));
    }
}
