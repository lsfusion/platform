package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
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
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.flow.FlowActionProperty;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;

public class AggChangeActionProperty<P extends PropertyInterface> extends CustomActionProperty {

    private final CalcProperty<P> aggProp; // assert что один интерфейс и aggProp
    private final ValueClass aggClass;

    private final ActionPropertyMapImplement<ClassPropertyInterface> changeAction; // WYSAction

    public AggChangeActionProperty(String sID, String caption, ValueClass[] classes, CalcProperty<P> aggProp, ValueClass aggClass, ActionPropertyMapImplement<ClassPropertyInterface> changeAction) {
        super(sID, caption, classes);
        this.aggProp = aggProp;
        this.aggClass = aggClass;
        this.changeAction = changeAction;
    }

    @Override
    public void executeCustom(final ExecutionContext context) throws SQLException {
        ObjectValue readValue = null;
                
        Type type = aggProp.getType();
        if(type instanceof DataClass)
            readValue = context.requestUserData((DataClass) type, null);
        else
            context.requestUserObject(new ExecutionContext.RequestDialog() {
                @Override
                public DialogInstance createDialog() throws SQLException {
                    return context.getFormInstance().createObjectDialog((CustomClass) aggProp.getValueClass());
                }
            });
        
        if(readValue==null)
            return;

        // пока тупо MGProp'им назад
        KeyExpr keyExpr = new KeyExpr("key");
        Expr groupExpr = GroupExpr.create(Collections.singletonMap(0, aggProp.getExpr(Collections.singletonMap(BaseUtils.single(aggProp.interfaces), keyExpr), context.getModifier())),
                keyExpr, keyExpr.isClass(aggClass.getUpSet()), GroupType.ANY, Collections.singletonMap(0, readValue.getExpr()));
        ObjectValue convertWYSValue = BaseUtils.singleValue(BaseUtils.singleValue(new Query<String, String>(new HashMap<String, KeyExpr>(), Collections.singletonMap("value", groupExpr)).executeClasses(context.getSession())));

        context.pushUserInput(convertWYSValue);
        FlowActionProperty.execute(context, changeAction);
        context.popUserInput(convertWYSValue);
    }
}
