package lsfusion.server.logics.property.actions.edit;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.DialogInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.AroundAspectActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;

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
