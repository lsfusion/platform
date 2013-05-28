package platform.server.logics.property.actions.edit;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.form.instance.DialogInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

public class AggChangeActionProperty<P extends PropertyInterface> extends AroundAspectActionProperty {

    private final CalcProperty<P> aggProp; // assert что один интерфейс и aggProp
    private final ValueClass aggClass;

    public AggChangeActionProperty(String sID, String caption, ImOrderSet<JoinProperty.Interface> listInterfaces, CalcProperty<P> aggProp, ValueClass aggClass, ActionPropertyMapImplement<?, JoinProperty.Interface> changeAction) {
        super(sID, caption, listInterfaces, changeAction);
        this.aggProp = aggProp;
        this.aggClass = aggClass;
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type type = aggProp.getType();
        return type instanceof DataClass ? type : null;
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException {
        ObjectValue readValue = null;

        Type type = aggProp.getType();
        if (type instanceof DataClass) {
            readValue = context.requestUserData((DataClass) type, null);
        } else {
            context.requestUserObject(new ExecutionContext.RequestDialog() {
                @Override
                public DialogInstance createDialog() throws SQLException {
                    return context.getFormInstance().createObjectDialog((CustomClass) aggProp.getValueClass());
                }
            });
        }

        if (readValue != null) {
            // пока тупо MGProp'им назад
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(
                    MapFact.singleton(0, aggProp.getExpr(MapFact.singleton(aggProp.interfaces.single(), keyExpr), context.getModifier())),
                    keyExpr,
                    keyExpr.isUpClass(aggClass),
                    GroupType.ANY,
                    MapFact.singleton(0, readValue.getExpr())
            );

            ImOrderMap<ImMap<String, DataObject>, ImMap<String, ObjectValue>> values =
                    new Query<String, String>(MapFact.<String, KeyExpr>EMPTYREV(), MapFact.singleton("value", groupExpr)).executeClasses(context);

            if (values.size() != 0) {
                ObjectValue convertWYSValue = values.singleValue().singleValue();
                return proceed(context.pushUserInput(convertWYSValue));
            }
        }
        return FlowResult.FINISH;
    }
}
