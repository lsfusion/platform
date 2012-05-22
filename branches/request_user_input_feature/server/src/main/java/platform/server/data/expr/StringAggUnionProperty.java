package platform.server.data.expr;

import platform.server.caches.IdentityLazy;
import platform.server.classes.DataClass;
import platform.server.classes.InsensitiveStringClass;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.FormulaUnionProperty;
import platform.server.logics.property.CalcPropertyMapImplement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringAggUnionProperty extends FormulaUnionProperty {

    private final String delimiter;
    private final List<CalcPropertyInterfaceImplement<Interface>> operands;

    public StringAggUnionProperty(String sID, String caption, List<Interface> interfaces, List<CalcPropertyInterfaceImplement<Interface>> operands, String delimiter) {
        super(sID, caption, interfaces);
        this.delimiter = delimiter;
        this.operands = operands;

        finalizeInit();
    }

    @IdentityLazy
    protected String getFormula() {
        String formula = "prm1";
        for(int i=1;i<operands.size();i++) {
            String prmName = "prm" + (i+1);
            formula = "CASE WHEN " + formula + " IS NOT NULL THEN (CASE WHEN " + prmName + " IS NOT NULL THEN " + formula + " || '" + delimiter + "' || " + prmName +
                    " ELSE " + formula + " END) ELSE " + prmName + " END";
        }
        return formula;
    }

    protected DataClass getDataClass() {
        return InsensitiveStringClass.get(20);
    }

    @IdentityLazy
    protected Map<String, CalcPropertyInterfaceImplement<Interface>> getParams() {
        Map<String, CalcPropertyInterfaceImplement<Interface>> params = new HashMap<String, CalcPropertyInterfaceImplement<Interface>>();
        for(int i=0;i<operands.size();i++)
            params.put("prm"+(i+1), operands.get(i));
        return params;
    }
}
