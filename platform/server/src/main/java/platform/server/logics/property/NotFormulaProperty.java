package platform.server.logics.property;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class NotFormulaProperty extends FormulaProperty<PropertyInterface> {

    public NotFormulaProperty() {
        super("NOT", "Не", SetFact.singletonOrder(new PropertyInterface(0)));
    }

    public final static NotFormulaProperty instance = new NotFormulaProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.singleValue().getWhere().not());
    }
    
    public <T> CalcPropertyImplement<PropertyInterface , T> getImplement(T map) {
        return new CalcPropertyImplement<PropertyInterface, T>(this, MapFact.singleton(interfaces.single(), map));
    } 
}
