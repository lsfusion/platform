package platform.server.logics.property.actions.flow;

import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.*;

public class ChangeActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, W, I> {

    private CalcPropertyInterfaceImplement<I> writeFrom;

    public ChangeActionProperty(String sID,
                                String caption,
                                Collection<I> innerInterfaces,
                                List<I> mapInterfaces, CalcPropertyMapImplement<W, I> where, CalcPropertyMapImplement<P, I> writeTo,
                                CalcPropertyInterfaceImplement<I> writeFrom) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, where, Collections.singletonList(writeFrom));

        this.writeFrom = writeFrom;

        finalizeInit();
    }

    protected void write(ExecutionContext context, Map<P, DataObject> toValues, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, Expr> innerExprs) throws SQLException {

        Expr writeExpr = writeFrom.mapExpr(innerExprs, context.getModifier());

        if(!isWhereFull())
            changeWhere = changeWhere.and(writeExpr.getWhere().or(
                    writeTo.property.getExpr(PropertyChange.getMapExprs(toKeys, toValues), context.getModifier()).getWhere()));

        PropertyChange<P> change = new PropertyChange<P>(toValues, toKeys, writeExpr, changeWhere);
        context.addActions(context.getEnv().change(writeTo.property, change)); // нет FormEnvironment так как заведомо не action
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        writeFrom.mapFillDepends(result);
        where.mapFillDepends(result);
        return result;
    }

    public Set<CalcProperty> getChangeProps() {
        return Collections.singleton((CalcProperty)writeTo.property);
//        return new HashSet<CalcProperty>(writeTo.property.getDataChanges());
    }

    @Override
    protected CalcPropertyMapImplement<?, I> getSetWhereProperty() {
        // проверяем на is WriteClass (можно было бы еще на интерфейсы проверить но пока нет смысла)
        return DerivedProperty.createUnion(innerInterfaces, DerivedProperty.createAnd(innerInterfaces, DerivedProperty.<I>createStatic(true, LogicalClass.instance), writeTo),
                    DerivedProperty.createJoin(IsClassProperty.getProperty(writeTo.property.getValueClass(), "value").
                        mapImplement(Collections.singletonMap("value", writeFrom))));
    }
}
