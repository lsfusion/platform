package platform.server.logics.properties;

import platform.server.data.query.exprs.FormulaExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.data.TableFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaPropertyInterface> {

    String formula;
    
    static Collection<StringFormulaPropertyInterface> getInterfaces(int intNum) {
        Collection<StringFormulaPropertyInterface> interfaces = new ArrayList<StringFormulaPropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new StringFormulaPropertyInterface(i));
        return interfaces;
    }

    public StringFormulaProperty(String sID,int intNum, TableFactory iTableFactory, RemoteClass iValue, String iFormula) {
        super(sID,getInterfaces(intNum),iTableFactory,iValue);
        formula = iFormula;
    }

    SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<StringFormulaPropertyInterface> joinClasses) {

        Map<String, SourceExpr> params = new HashMap<String, SourceExpr>();
        for(StringFormulaPropertyInterface propertyInterface : interfaces)
            params.put("prm"+(propertyInterface.ID+1), joinImplement.get(propertyInterface));

        return new FormulaExpr(formula, value.getType(),params);
    }

    RemoteClass getOperandClass() {
        return RemoteClass.data;
    }
}
