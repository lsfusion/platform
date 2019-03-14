package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.language.ScriptingLogicsModule;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public abstract class ExternalAction extends SystemAction {

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

    public ExternalAction(int exParams, ImList<Type> params, ImList<LP> targetPropList) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + exParams, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        this.paramInterfaces = orderInterfaces.subOrder(exParams, orderInterfaces.size());
        paramTypes = paramInterfaces.mapList(params);
        this.targetPropList = targetPropList;
    }

    private Type getParamType(PropertyInterface paramInterface, ObjectValue value) {
        if(value instanceof DataObject)
            return ((DataObject) value).getType();

        Type type = paramTypes.get(paramInterface);
        if(type != null)
            return type;

        return AbstractType.getUnknownTypeNull();
    }

    protected Object format(ExecutionContext<PropertyInterface> context, PropertyInterface paramInterface, Charset urlEncodeCharset) {
        ObjectValue value = context.getKeyValue(paramInterface);
        Object result = getParamType(paramInterface, value).formatHTTP(value.getValue(), urlEncodeCharset);
        if(result instanceof String && urlEncodeCharset != null)
            try {
                result = URLEncoder.encode((String) result, urlEncodeCharset.name());
            } catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }
        return result;
    }

    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString) {
        return replaceParams(context, connectionString, null, null);
    }
    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString, Result<ImOrderSet<PropertyInterface>> rNotUsedParams, Charset urlEncodeCharset) {
        ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
        MOrderExclSet<PropertyInterface> mNotUsedParams = rNotUsedParams != null ? SetFact.<PropertyInterface>mOrderExclSetMax(orderInterfaces.size()) : null;
        for (int i = 0, size = orderInterfaces.size(); i < size ; i++) {
            String prmName = getParamName(String.valueOf(i + 1));
            PropertyInterface paramInterface = orderInterfaces.get(i);
            Object replacement = format(context, paramInterface, urlEncodeCharset);
            if (replacement instanceof FileData ||
                    (mNotUsedParams != null && !connectionString.contains(prmName))) {
                if(mNotUsedParams != null)
                    mNotUsedParams.exclAdd(paramInterface);
                continue; // можно не делать continue - это оптимизация
            }
            connectionString = connectionString.replace(prmName, (String)replacement);
        }
        if(rNotUsedParams != null)
            rNotUsedParams.set(mNotUsedParams.immutableOrder());
        return connectionString;
    }

    public static ImMap<Property, Boolean> getChangeExtProps(ImList<LP> props) {
        return props.mapListValues(new GetValue<Property, LP>() {
            public Property getMapValue(LP value) {
                return ((LP<?>)value).property;
            }}).toOrderSet().getSet().toMap(false);
    }
    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeExtProps(targetPropList);
    }
}