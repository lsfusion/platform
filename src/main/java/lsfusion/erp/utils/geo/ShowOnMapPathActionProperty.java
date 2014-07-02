package lsfusion.erp.utils.geo;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class ShowOnMapPathActionProperty extends ScriptingActionProperty {

    public ShowOnMapPathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LCP<PropertyInterface> isPOI = (LCP<PropertyInterface>) is(getClass("POI"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isPOI.getMapKeys();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("latitude", getLCP("latitudePOI").getExpr(keys.singleValue()));
            query.addProperty("longitude", getLCP("longitudePOI").getExpr(keys.singleValue()));
            query.addProperty("numberPathPOI", getLCP("numberPathPOI").getExpr(context.getModifier(), keys.singleValue()));
            query.addProperty("namePOI", getLCP("namePOI").getExpr(keys.singleValue()));
            query.addProperty("descriptionPathPOI", getLCP("descriptionPathPOI").getExpr(context.getModifier(), keys.singleValue()));
            query.and(isPOI.property.getExpr(keys).getWhere());
            query.and(getLCP("numberPathPOI").getExpr(context.getModifier(), keys.singleValue()).getWhere());
            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(context, MapFact.singletonOrder((Object) "numberPathPOI", false));
            String uri = "http://maps.google.com/?";
            int index = 1;
            for (ImMap<Object, Object> values : result.valueIt()) {
                BigDecimal latitude = (BigDecimal) values.get("latitude");
                BigDecimal longitude = (BigDecimal) values.get("longitude");
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
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }

    }
}
