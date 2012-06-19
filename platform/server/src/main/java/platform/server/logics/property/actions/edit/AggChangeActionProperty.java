package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.singleValue;

public class AggChangeActionProperty<P extends PropertyInterface> extends AroundAspectActionProperty {

    private final CalcProperty<P> aggProp; // assert что один интерфейс и aggProp
    private final ValueClass aggClass;

    public AggChangeActionProperty(String sID, String caption, List<JoinProperty.Interface> listInterfaces, CalcProperty<P> aggProp, ValueClass aggClass, ActionPropertyMapImplement<?, JoinProperty.Interface> changeAction) {
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
                    singletonMap(0, aggProp.getExpr(singletonMap(BaseUtils.single(aggProp.interfaces), keyExpr), context.getModifier())),
                    keyExpr,
                    keyExpr.isClass(aggClass.getUpSet()),
                    GroupType.ANY,
                    singletonMap(0, readValue.getExpr())
            );

            OrderedMap<Map<String,DataObject>,Map<String,ObjectValue>> values =
                    new Query<String, String>(new HashMap<String, KeyExpr>(), singletonMap("value", groupExpr)).executeClasses(context);

            if (values.size() != 0) {
                ObjectValue convertWYSValue = singleValue(singleValue(values));
                return proceed(context.pushUserInput(convertWYSValue));
            }
        }
        return FlowResult.FINISH;
    }
}
