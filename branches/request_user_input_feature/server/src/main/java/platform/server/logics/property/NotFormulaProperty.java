package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NotFormulaProperty extends FormulaProperty<PropertyInterface> {

    public NotFormulaProperty() {
        super("NOT", "Не", Collections.singletonList(new PropertyInterface(0)));
    }

    public final static NotFormulaProperty instance = new NotFormulaProperty();

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(BaseUtils.singleValue(joinImplement).getWhere().not());
    }
    
    public <T> CalcPropertyImplement<PropertyInterface , T> getImplement(T map) {
        return new CalcPropertyImplement<PropertyInterface, T>(this, Collections.singletonMap(BaseUtils.single(interfaces), map));
    } 
}
