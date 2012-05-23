package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.interop.form.ServerResponse;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

public class ChangeActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, W, I> {

    private CalcPropertyInterfaceImplement<I> writeFrom;
    private final boolean useEditAction;

    public ChangeActionProperty(String sID,
                                String caption,
                                Collection<I> innerInterfaces,
                                List<I> mapInterfaces, CalcPropertyMapImplement<W, I> where, CalcPropertyMapImplement<P, I> writeTo,
                                CalcPropertyInterfaceImplement<I> writeFrom, boolean useEditAction) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, where);

        this.writeFrom = writeFrom;
        this.useEditAction = useEditAction;

        finalizeInit();
    }

    protected void write(ExecutionContext<PropertyInterface> context, Map<P, DataObject> toValues, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, Expr> innerExprs) throws SQLException {

        Expr writeExpr = writeFrom.mapExpr(innerExprs, context.getModifier());

        if(useEditAction) {
            context.addActions(getWriteAction().execute(new PropertyChange<P>(toValues, toKeys, writeExpr, changeWhere), context.getEnv(), context.getForm().map(BaseUtils.crossValues(writeTo.mapping, mapInterfaces))));
            return;
        }

        if(!isWhereFull())
            changeWhere = changeWhere.and(writeExpr.getWhere().or(
                    writeTo.property.getExpr(PropertyChange.getMapExprs(toKeys, toValues), context.getModifier()).getWhere()));

        context.addActions(context.getEnv().change(writeTo.property, new PropertyChange<P>(toValues, toKeys, writeExpr, changeWhere))); // нет FormEnvironment так как заведомо не action
    }

    private ActionPropertyMapImplement<?, P> getWriteAction() {
        return writeTo.property.getEditAction(ServerResponse.CHANGE);
    }

    @Override
    public Set<ActionProperty> getDependActions() {
        if(useEditAction)
            return Collections.singleton((ActionProperty)getWriteAction().property);

        return super.getDependActions();
    }

    @Override
    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result;
        if(useEditAction)
            result = new HashSet<CalcProperty>(super.getUsedProps());
        else
            result = new HashSet<CalcProperty>();
        writeFrom.mapFillDepends(result);

        where.mapFillDepends(result);
        return result;
    }

    @Override
    public Set<CalcProperty> getChangeProps() {
        if(useEditAction)
            return super.getChangeProps();

        return new HashSet<CalcProperty>(writeTo.property.getChangeProps());
    }

    @Override
    protected CalcPropertyMapImplement<?, I> getSetWhereProperty() {
        if(useEditAction)
            return getWriteAction().mapWhereProperty().map(writeTo.mapping);

        // проверяем на is WriteClass (можно было бы еще на интерфейсы проверить но пока нет смысла)
        return DerivedProperty.createUnion(innerInterfaces, DerivedProperty.createNotNull(writeTo),
                    DerivedProperty.createJoin(IsClassProperty.getProperty(writeTo.property.getValueClass(), "value").
                        mapImplement(Collections.singletonMap("value", writeFrom))));
    }
}
