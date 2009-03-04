package platform.server.logics.properties;

import platform.server.data.query.exprs.FormulaExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.data.TableFactory;

import java.util.HashMap;
import java.util.Map;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaPropertyInterface> {

    String formula;

    public StringFormulaProperty(TableFactory iTableFactory, RemoteClass iValue, String iFormula) {
        super(iTableFactory,iValue);
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
