package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.data.TableFactory;
import platform.server.where.Where;

import java.util.Map;

public class NotNullFormulaProperty extends WhereFormulaProperty {

    public FormulaPropertyInterface property;

    public NotNullFormulaProperty(String sID, TableFactory iTableFactory) {
        super(sID,1,iTableFactory);
        property = interfaces.iterator().next();
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement) {
        return joinImplement.get(property).getWhere();
    }
}
