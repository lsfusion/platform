package lsfusion.server.logics.property.actions.edit;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.AroundAspectActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

public class AggChangeActionProperty<P extends PropertyInterface> extends AroundAspectActionProperty {

    private final CalcProperty<P> aggProp; // assert что один интерфейс и aggProp
    private final ValueClass aggClass;

    public AggChangeActionProperty(String caption, ImOrderSet<JoinProperty.Interface> listInterfaces, CalcProperty<P> aggProp, ValueClass aggClass, ActionPropertyMapImplement<?, JoinProperty.Interface> changeAction) {
        super(caption, listInterfaces, changeAction);
        this.aggProp = aggProp;
        this.aggClass = aggClass;
        
        finalizeInit();
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type type = aggProp.getType();
        return type instanceof DataClass ? type : null;
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue readValue = null;

        Type type = aggProp.getType();
        if (type instanceof DataClass) {
            readValue = context.requestUserData((DataClass) type, null);
        } else {
            context.requestUserObject(
                    context.getFormInstance().createObjectDialogRequest((CustomClass) aggProp.getValueClass(ClassType.editPolicy), context.stack)
            );
        }

        if (readValue != null) {
            // пока тупо MGProp'им назад
            KeyExpr keyExpr = new KeyExpr("key");
            Expr aggExpr = aggProp.getExpr(MapFact.singleton(aggProp.interfaces.single(), keyExpr), context.getModifier());
            Expr groupExpr;
            if(readValue.isNull()) {
                groupExpr = GroupExpr.create(
                        MapFact.<String, Expr>EMPTY(),
                        keyExpr,
                        keyExpr.isUpClass(aggClass).and(aggExpr.getWhere().not()),
                        GroupType.ANY,
                        MapFact.<String, Expr>EMPTY()
                );
            } else {
                groupExpr = GroupExpr.create(
                        MapFact.singleton(0, aggExpr),
                        keyExpr,
                        keyExpr.isUpClass(aggClass),
                        GroupType.ANY,
                        MapFact.singleton(0, readValue.getExpr())
                );
            }

            ImOrderMap<ImMap<String, DataObject>, ImMap<String, ObjectValue>> values =
                    new Query<>(MapFact.<String, KeyExpr>EMPTYREV(), MapFact.singleton("value", groupExpr), Where.TRUE).executeClasses(context);

            ObjectValue convertWYSValue = values.singleValue().singleValue();
            return proceed(context.pushUserInput(convertWYSValue));
        }
        return FlowResult.FINISH;
    }
}
