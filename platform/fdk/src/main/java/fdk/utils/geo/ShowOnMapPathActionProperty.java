package fdk.utils.geo;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.action.MessageClientAction;
import platform.interop.action.OpenUriClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
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

public class ShowOnMapPathActionProperty extends ScriptingActionProperty {

    public ShowOnMapPathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            LCP<PropertyInterface> isPOI = (LCP<PropertyInterface>) LM.is(LM.getClassByName("POI"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isPOI.getMapKeys();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("latitude", LM.getLCPByName("latitudePOI").getExpr(keys.singleValue()));
            query.addProperty("longitude", LM.getLCPByName("longitudePOI").getExpr(keys.singleValue()));
            query.addProperty("numberPathPOI", LM.getLCPByName("numberPathPOI").getExpr(context.getModifier(), keys.singleValue()));
            query.addProperty("namePOI", LM.getLCPByName("namePOI").getExpr(keys.singleValue()));
            query.addProperty("descriptionPathPOI", LM.getLCPByName("descriptionPathPOI").getExpr(context.getModifier(), keys.singleValue()));
            query.and(isPOI.property.getExpr(keys).getWhere());
            query.and(LM.getLCPByName("numberPathPOI").getExpr(context.getModifier(), keys.singleValue()).getWhere());
            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(context.getSession().sql, MapFact.singletonOrder((Object) "numberPathPOI", false));
            String uri = "http://maps.google.com/?";
            int index = 1;
            for (ImMap<Object, Object> values : result.valueIt()) {
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
