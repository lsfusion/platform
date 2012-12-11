package platform.fdk.actions.geo;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.action.MessageClientAction;
import platform.interop.action.OpenUriClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;

import static java.util.Arrays.asList;

public class ShowOnMapPathActionProperty extends ScriptingActionProperty {

    public ShowOnMapPathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            LCP<PropertyInterface> isPOI = (LCP<PropertyInterface>) LM.is(LM.getClassByName("POI"));
            Map<PropertyInterface, KeyExpr> keys = isPOI.getMapKeys();
            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(keys);
            query.properties.put("latitude", LM.getLCPByName("latitudePOI").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("longitude", LM.getLCPByName("longitudePOI").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("numberPathPOI", LM.getLCPByName("numberPathPOI").getExpr(context.getModifier(), BaseUtils.singleValue(keys)));
            query.properties.put("namePOI", LM.getLCPByName("namePOI").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("descriptionPathPOI", LM.getLCPByName("descriptionPathPOI").getExpr(context.getModifier(), BaseUtils.singleValue(keys)));
            query.and(isPOI.property.getExpr(keys).getWhere());
            query.and(LM.getLCPByName("numberPathPOI").getExpr(context.getModifier(), BaseUtils.singleValue(keys)).getWhere());
            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> result = query.execute(context.getSession().sql, new OrderedMap(asList("numberPathPOI"), false));
            String uri = "http://maps.google.com/?";
            int index = 1;
            for (Map<Object, Object> values : result.values()) {
                Double latitude = (Double) values.get("latitude");
                Double longitude = (Double) values.get("longitude");
                String name = (String) values.get("namePOI");
                String description = (String) values.get("descriptionPathPOI");
                String overDescription = description==null ? name : description;
                if (latitude != null && longitude != null && overDescription!=null) {
                    String prefix = index == 1 ? "saddr=" : (index == result.values().size() ? "&daddr=": "+to:");
                    uri += prefix + overDescription.trim().replace(" ", "+").replace("\"", "") + "@" + latitude + "+" + longitude;
                    index++;
                }
            }

            if (index <= result.values().size())
                context.requestUserInteraction(new MessageClientAction("Не все координаты проставлены", "Ошибка"));
            else
                context.requestUserInteraction(new OpenUriClientAction(new URI(uri)));

        } catch (SQLException e) {
        } catch (URISyntaxException e) {
        }

    }
}
