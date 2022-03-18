package lsfusion.server.physics.dev.integration.internal.to;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.CustomFormulaImpl;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.data.FormulaImplProperty;
import lsfusion.server.logics.property.classes.data.ValueFormulaProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaProperty.Interface> {

    private final CustomFormulaSyntax formula;
    private final boolean valueNull;
    private final boolean paramsNull;
    
    public static String getParamName(String prmID) {
        return "prm" + prmID;
    }

    public static class Interface extends PropertyInterface {

        private String getString() {
            return getParamName(String.valueOf(ID+1));
        }

        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int paramCount) {
        return SetFact.toOrderExclSet(paramCount, Interface::new);
    }

    public Interface findInterface(String string) {
        for(Interface propertyInterface : interfaces)
            if(propertyInterface.getString().equals(string))
                return propertyInterface;
        throw new RuntimeException("not found");
    }

    public StringFormulaProperty(DataClass valueClass, CustomFormulaSyntax formula, int paramCount, boolean valueNull) {
        super(LocalizedString.create(formula.getDefaultSyntax()),getInterfaces(paramCount),valueClass);
        this.formula = formula;
        this.valueNull = valueNull;
        this.paramsNull = false;

        finalizeInit();
    }

    public Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {

        ImMap<String, Expr> params = interfaces.mapKeyValues(Interface::getString, joinImplement::get);

        return FormulaExpr.createCustomFormula(formula, paramsNull, value, params, valueNull);
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        // here it doesn't matter if it is union or join
        CustomFormulaImpl formulaImpl = FormulaExpr.createJoinCustomFormulaImpl(formula, value, valueNull, getOrderInterfaces().mapOrderSetValues(Interface::getString));
        return FormulaImplProperty.inferValueClass(getOrderInterfaces(), formulaImpl, inferred);
    }
}
