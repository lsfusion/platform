package platform.server.logics.property;

import platform.server.classes.StaticClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Map;

public class ValueProperty extends NoIncrementProperty<PropertyInterface> {

    public final Object value;
    public final StaticClass staticClass;

    public ValueProperty(String sID, String caption, Object value, StaticClass staticClass) {
        super(sID, caption, new ArrayList<PropertyInterface>());
        this.value = value;
        this.staticClass = staticClass;

        finalizeInit();
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return staticClass.getStaticExpr(value);
    }
}
