package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ConcreteValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaProperty.Interface> {

    final String formula;

    public static class Interface extends PropertyInterface {

        private String getString() {
            return "prm"+(ID+1);
        }

        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int paramCount) {
        return SetFact.toOrderExclSet(paramCount, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }});
    }

    public Interface findInterface(String string) {
        for(Interface propertyInterface : interfaces)
            if(propertyInterface.getString().equals(string))
                return propertyInterface;
        throw new RuntimeException("not found");
    }

    public StringFormulaProperty(String sID, ConcreteValueClass valueClass, String formula, int paramCount) {
        super(sID,formula,getInterfaces(paramCount),valueClass);
        this.formula = formula;

        finalizeInit();
    }

    public Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {

        ImMap<String, Expr> params = interfaces.mapKeyValues(new GetValue<String, Interface>() {
            public String getMapValue(Interface value) {
                return value.getString();
            }}, new GetValue<Expr, Interface>() {
            public Expr getMapValue(Interface value) {
                return joinImplement.get(value);
            }});

        return FormulaExpr.createCustomFormula(formula, value, params);
    }
}
