package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.data.TableFactory;
import platform.server.where.Where;

import java.util.Map;
import java.util.Iterator;

public class CompareFormulaProperty extends WhereFormulaProperty {

    int compare;
    public FormulaPropertyInterface operator1;
    public FormulaPropertyInterface operator2;

    public CompareFormulaProperty(String sID, TableFactory iTableFactory, int iCompare) {
        super(sID,2,iTableFactory);
        compare = iCompare;

        Iterator<FormulaPropertyInterface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement) {
        return new CompareWhere(joinImplement.get(operator1), joinImplement.get(operator2), compare);
    }
}
