package lsfusion.server.logics.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class NotFormulaProperty extends FormulaProperty<PropertyInterface> {

    public NotFormulaProperty() {
        super("NOT", "Не", SetFact.singletonOrder(new PropertyInterface(0)));
    }

    public final static NotFormulaProperty instance = new NotFormulaProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.singleValue().getWhere().not());
    }
    
    public <T> CalcPropertyImplement<PropertyInterface , T> getImplement(T map) {
        return new CalcPropertyImplement<PropertyInterface, T>(this, MapFact.singleton(interfaces.single(), map));
    } 
}
