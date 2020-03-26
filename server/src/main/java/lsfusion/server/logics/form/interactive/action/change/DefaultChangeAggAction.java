package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.AroundAspectAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class DefaultChangeAggAction<P extends PropertyInterface> extends AroundAspectAction {

    private final Property<P> aggProp; // assert что один интерфейс и aggProp
    private final ValueClass aggClass;

    public <I extends PropertyInterface> DefaultChangeAggAction(LocalizedString caption, ImOrderSet<I> listInterfaces, Property<P> aggProp, ValueClass aggClass, ActionMapImplement<?, I> changeAction) {
        super(caption, listInterfaces, changeAction);
        this.aggProp = aggProp;
        this.aggClass = aggClass;
        
        finalizeInit();
    }

    @Override // сам выполняет request поэтому на inRequest не смотрим
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type type = aggProp.getType();
        return type instanceof DataClass ? type : null;
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue readValue = null;

        Type type = aggProp.getType();
        if (type instanceof DataClass) {
            readValue = context.requestUserData((DataClass) type, null, false);
        } else {
            context.inputUserObject(
                    context.getFormFlowInstance().createObjectDialogRequest((CustomClass) aggProp.getValueClass(ClassType.editValuePolicy), context.stack)
            );
        }

        if (readValue != null) {
            // пока тупо MGProp'им назад
            KeyExpr keyExpr = new KeyExpr("key");
            Expr aggExpr = aggProp.getExpr(MapFact.singleton(aggProp.interfaces.single(), keyExpr), context.getModifier());
            Expr groupExpr;
            GroupType groupType = GroupType.ASSERTSINGLE_CHANGE();
            if(readValue.isNull()) {
                groupExpr = GroupExpr.create(
                        MapFact.EMPTY(),
                        keyExpr,
                        keyExpr.isUpClass(aggClass).and(aggExpr.getWhere().not()),
                        groupType,
                        MapFact.EMPTY()
                );
            } else {
                groupExpr = GroupExpr.create(
                        MapFact.singleton(0, aggExpr),
                        keyExpr,
                        keyExpr.isUpClass(aggClass),
                        groupType,
                        MapFact.singleton(0, readValue.getExpr())
                );
            }

            ObjectValue convertWYSValue = Expr.readObjectValue(context.getSession().sql, context.getSession().baseClass, groupExpr, context.getQueryEnv());
            return context.pushRequestedValue(convertWYSValue, aggClass.getType(), () -> proceed(context));
        }
        return FlowResult.FINISH;
    }

    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createDefaultChangedAggAction(interfaces, aggProp, aggClass, action);
    }
}
