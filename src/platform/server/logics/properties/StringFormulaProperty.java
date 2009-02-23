package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.DataClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.FormulaExpr;

import java.util.Map;
import java.util.HashMap;

public class StringFormulaProperty extends ValueFormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    public StringFormulaProperty(TableFactory iTableFactory, DataClass iValue, String iFormula) {
        super(iTableFactory,iValue);
        Formula = iFormula;
    }

    SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<StringFormulaPropertyInterface> joinClasses) {

        Map<String, SourceExpr> Params = new HashMap<String, SourceExpr>();
        for(StringFormulaPropertyInterface Interface : interfaces)
            Params.put("prm"+(Interface.ID+1), joinImplement.get(Interface));

        return new FormulaExpr(Formula,Value.getType(),Params);
    }

    DataClass getOperandClass() {
        return DataClass.data;
    }
}
