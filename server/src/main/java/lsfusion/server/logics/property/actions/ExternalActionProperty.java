package lsfusion.server.logics.property.actions;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import java.util.List;

public abstract class ExternalActionProperty extends SystemActionProperty {

    protected String connectionString;
    protected List<LCP> targetPropList;
    
    // важно! используется также в regexp'е, то есть не должно быть спецсимволов
    public static String getParamName(String prmID) {
        return "qxprm" + prmID + "nx";
    }

    public ExternalActionProperty(int paramsCount, String connectionString, List<LCP> targetPropList) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(paramsCount, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        this.connectionString = connectionString;
        this.targetPropList = targetPropList;
    }

    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString) {
        ImMap<PropertyInterface, ? extends ObjectValue> values = context.getKeys();
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        for (int i = orderInterfaces.size(); i > 0; i--) {
            String regex = "\\(" + getParamName(String.valueOf(i)) + "\\)";
            ObjectValue value = values.getValue(i - 1);
            if(!(value instanceof DataObject && ((DataObject) value).objectClass instanceof DynamicFormatFileClass)) {
                String replacement = String.valueOf(value.getValue()); // переделать на format ???
                connectionString = connectionString.replaceAll(regex, replacement);
            }
        }
        try {
            return URIUtil.encodeQuery(connectionString, "UTF-8");
        } catch (URIException e) {
            ServerLoggers.systemLogger.error("ReplaceParams error: ", e);
            return connectionString;
        }
    }
}