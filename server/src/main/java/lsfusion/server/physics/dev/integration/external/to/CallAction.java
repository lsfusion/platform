package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class CallAction extends SystemAction {

    protected ImOrderSet<PropertyInterface> paramInterfaces;
    protected ImMap<PropertyInterface, Type> paramTypes;
    protected ImList<LP> targetPropList;

    protected static String getTransformedText(ExecutionContext<PropertyInterface> context, PropertyInterface param) {
        return ScriptingLogicsModule.transformFormulaText((String)context.getKeyObject(param), getParamName("$1"));
    }

    // важно! используется также в regexp'е, то есть не должно быть спецсимволов
    protected static String getParamName(String prmID) {
        return "qxprm" + prmID + "nx";
    }

    public CallAction(int exParams, ImList<Type> params, ImList<LP> targetPropList) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + exParams, i -> new PropertyInterface()));
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        this.paramInterfaces = orderInterfaces.subOrder(exParams, orderInterfaces.size());
        paramTypes = paramInterfaces.mapList(params);
        this.targetPropList = targetPropList;
    }

    protected Type getParamType(PropertyInterface paramInterface, ObjectValue value) {
        if(value instanceof DataObject)
            return ((DataObject) value).getType();

        Type type = paramTypes.get(paramInterface);
        if(type != null)
            return type;

        return AbstractType.getUnknownTypeNull();
    }

    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString) {
        ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
        for (int i = 0, size = orderInterfaces.size(); i < size ; i++) {
            PropertyInterface paramInterface = orderInterfaces.get(i);
            ObjectValue objectValue = context.getKeyValue(paramInterface);
            Object value = getParamType(paramInterface, objectValue).formatString(objectValue.getValue());

            connectionString = connectionString.replace(getParamName(String.valueOf(i + 1)), (String) value);
        }
        return connectionString;
    }

    public static ImMap<Property, Boolean> getChangeExtProps(ImList<LP> props) {
        return props.mapListValues((LP value) -> value.property).toOrderSet().getSet().toMap(false);
    }
    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeExtProps(targetPropList);
    }
}