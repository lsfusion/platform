package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

public class SetActionProperty<P extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, I> {

    private boolean notNull;

    public SetActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<P, I> writeTo, boolean notNull) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, new ArrayList<PropertyInterfaceImplement<I>>());

        this.notNull = notNull;

        finalizeInit();
    }

    @Override
    protected void write(ExecutionContext context, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, PropertyObjectInterfaceInstance> innerObjects, Map<I, Expr> fromKeys) throws SQLException {
        if(notNull)
            writeTo.property.setNotNull(toKeys, changeWhere, context.getSession(), context.getModifier());
        else
            writeTo.property.setNull(toKeys, changeWhere, context.getSession(), context.getModifier());
    }

    protected Collection<Property> getWriteProps() {
        return writeTo.property.getSetChangeProps(notNull, false);
    }

    public Set<Property> getUsedProps() {
        return new HashSet<Property>();
    }
}
