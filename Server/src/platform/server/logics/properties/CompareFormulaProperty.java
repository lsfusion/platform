package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.data.TableFactory;
import platform.server.where.Where;

import java.util.Map;

public class CompareFormulaProperty extends WhereFormulaProperty {

    int Compare;
    public FormulaPropertyInterface operator1;
    public FormulaPropertyInterface operator2;

    public CompareFormulaProperty(TableFactory iTableFactory, int iCompare) {
        super(iTableFactory);
        Compare = iCompare;
        operator1 = new FormulaPropertyInterface(0);
        operator2 = new FormulaPropertyInterface(1);
        interfaces.add(operator1);
        interfaces.add(operator2);
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement) {
        return new CompareWhere(JoinImplement.get(operator1),JoinImplement.get(operator2),Compare);
    }
}
