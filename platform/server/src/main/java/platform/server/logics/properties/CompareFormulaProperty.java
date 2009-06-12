package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.Iterator;
import java.util.Map;

public class CompareFormulaProperty extends WhereFormulaProperty {

    int compare;
    public FormulaPropertyInterface operator1;
    public FormulaPropertyInterface operator2;

    public CompareFormulaProperty(String sID, int iCompare) {
        super(sID,2);
        compare = iCompare;

        Iterator<FormulaPropertyInterface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();
    }

    Where getWhere(Map<FormulaPropertyInterface,? extends SourceExpr> joinImplement) {
        return joinImplement.get(operator1).compare(joinImplement.get(operator2), compare);
    }
}
