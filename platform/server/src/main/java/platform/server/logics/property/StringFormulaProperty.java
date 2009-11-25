package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.data.expr.Expr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.WhereBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaPropertyInterface> {

    String formula;
    
    static Collection<StringFormulaPropertyInterface> getInterfaces(int paramCount) {
        Collection<StringFormulaPropertyInterface> interfaces = new ArrayList<StringFormulaPropertyInterface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new StringFormulaPropertyInterface(i));
        return interfaces;
    }

    public StringFormulaProperty(String sID, ConcreteValueClass iValue, String formula, int paramCount) {
        super(sID,formula,getInterfaces(paramCount),iValue);
        this.formula = formula;
    }

    public Expr calculateExpr(Map<StringFormulaPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Map<String, Expr> params = new HashMap<String, Expr>();
        for(StringFormulaPropertyInterface propertyInterface : interfaces)
            params.put("prm"+(propertyInterface.ID+1), joinImplement.get(propertyInterface));

        return Expr.formula(formula,value,params);
    }
}
