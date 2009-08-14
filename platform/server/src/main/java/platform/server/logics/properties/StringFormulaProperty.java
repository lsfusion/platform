package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

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

    public StringFormulaProperty(String sID, ConcreteValueClass iValue, String iFormula, int paramCount) {
        super(sID,getInterfaces(paramCount),iValue);
        formula = iFormula;
    }

    public SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {

        Map<String, SourceExpr> params = new HashMap<String, SourceExpr>();
        for(StringFormulaPropertyInterface propertyInterface : interfaces)
            params.put("prm"+(propertyInterface.ID+1), joinImplement.get(propertyInterface));

        return SourceExpr.formula(formula,value,params);
    }
}
