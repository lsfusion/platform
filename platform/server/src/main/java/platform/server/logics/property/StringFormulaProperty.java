package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaExpr;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.data.where.WhereBuilder;

import java.util.*;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaProperty.Interface> {

    String formula;

    public static class Interface extends PropertyInterface {

        private String getString() {
            return "prm"+(ID+1);
        }

        public Interface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int paramCount) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public Interface findInterface(String string) {
        for(Interface propertyInterface : interfaces)
            if(propertyInterface.getString().equals(string))
                return propertyInterface;
        throw new RuntimeException("not found");
    }

    public StringFormulaProperty(String sID, ConcreteValueClass iValue, String formula, int paramCount) {
        super(sID,formula,getInterfaces(paramCount),iValue);
        this.formula = formula;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        Map<String, Expr> params = new HashMap<String, Expr>();
        for(Interface propertyInterface : interfaces)
            params.put(propertyInterface.getString(), joinImplement.get(propertyInterface));

        return FormulaExpr.create(formula, value, params);
    }
}
