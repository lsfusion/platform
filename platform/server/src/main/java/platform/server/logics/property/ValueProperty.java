package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.StaticClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class ValueProperty extends NoIncrementProperty<PropertyInterface> {

    public final Object value;
    public final StaticClass staticClass;

    public ValueProperty(String sID, String caption, Object value, StaticClass staticClass) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.value = value;
        this.staticClass = staticClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return staticClass.getStaticExpr(value);
    }
}
