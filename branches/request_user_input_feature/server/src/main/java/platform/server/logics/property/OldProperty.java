package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.PropertyChanges;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OldProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {
    public final CalcProperty<T> property;

    public OldProperty(CalcProperty<T> property) {
        super("OLD_" + property.getSID(), property.caption + " (в БД)", (List<T>)property.interfaces);
        this.property = property;
    }

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return property.getClassValueWhere();
    }

    @Override
    public Set<OldProperty> getOldDepends() {
        return Collections.<OldProperty>singleton(this);
    }

    protected Expr calculateExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses)
            return getClassTableExpr(joinImplement);

        return property.getExpr(joinImplement); // возвращаем старое значение
    }
}
