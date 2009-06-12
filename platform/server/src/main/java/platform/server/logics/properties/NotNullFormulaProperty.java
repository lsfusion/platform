package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.Map;

public class NotNullFormulaProperty extends WhereFormulaProperty {

    public FormulaPropertyInterface property;

    public NotNullFormulaProperty(String sID) {
        super(sID,1);
        property = interfaces.iterator().next();
    }

    Where getWhere(Map<FormulaPropertyInterface,? extends SourceExpr> joinImplement) {
        return joinImplement.get(property).getWhere();
    }
}
