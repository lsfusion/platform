package platform.server.data.expr;

import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.caches.IdentityLazy;
import platform.server.classes.DataClass;
import platform.server.classes.InsensitiveStringClass;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.FormulaUnionProperty;

public class StringAggUnionProperty extends FormulaUnionProperty {

    private final String delimiter;
    private final ImList<CalcPropertyInterfaceImplement<Interface>> operands;

    public StringAggUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands, String delimiter) {
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
        return InsensitiveStringClass.get(200);
    }

    @IdentityLazy
    protected ImMap<String, CalcPropertyInterfaceImplement<Interface>> getParams() {
        return operands.getCol().mapColKeys(new GetIndex<String>() {
            public String getMapValue(int i) {
                return "prm"+(i+1);
            }});
    }
}
