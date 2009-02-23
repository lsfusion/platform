package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Map;

public class NotNullFormulaProperty extends WhereFormulaProperty {

    public FormulaPropertyInterface property;

    public NotNullFormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
        property = new FormulaPropertyInterface(0);
        interfaces.add(property);
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement) {
        return JoinImplement.get(property).getWhere();
    }
}
