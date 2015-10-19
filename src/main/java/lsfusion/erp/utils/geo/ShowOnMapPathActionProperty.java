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
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;

public class ShowOnMapPathActionProperty extends GeoActionProperty {
    private final ClassPropertyInterface mapProviderInterface;

    public ShowOnMapPathActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        mapProviderInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);
            boolean isYandex = isYandex(context, mapProvider);

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitudePOI").getExpr(poiExpr));
            query.addProperty("longitude", findProperty("longitudePOI").getExpr(poiExpr));
            query.addProperty("numberPathPOI", findProperty("numberPathPOI").getExpr(context.getModifier(), poiExpr));
            query.addProperty("namePOI", findProperty("namePOI").getExpr(poiExpr));
            query.and(findProperty("numberPathPOI").getExpr(context.getModifier(), poiExpr).getWhere());
            ImOrderMap<ImMap<String, Object>, ImMap<Object, Object>> result = query.execute(context, MapFact.singletonOrder((Object) "numberPathPOI", false));
            String uri = "";
            int index = 1;
            String firstLatLong = null;
            for (ImMap<Object, Object> values : result.valueIt()) {
                BigDecimal latitude = (BigDecimal) values.get("latitude");
                BigDecimal longitude = (BigDecimal) values.get("longitude");
                String name = (String) values.get("namePOI");
                String description = (String) values.get("descriptionPathPOI");
                String overDescription = description==null ? name : description;
                if (latitude != null && longitude != null && overDescription!=null) {
                    String latLong = latitude + (isYandex ? "%2C" : ",") + longitude;
                    uri += latLong + (isYandex ? "~" : "/");
                    if(index == 1)
                        firstLatLong = latLong;
                    index++;
                }
            }

            if (index <= result.values().size())
                context.requestUserInteraction(new MessageClientAction("Не все координаты проставлены", "Ошибка"));
            else
                context.requestUserInteraction(new OpenUriClientAction(new URI((isYandex ?
                        ("https://maps.yandex.ru/?rtt=auto&rtm=atm&rtext=" + uri + firstLatLong) :
                        ("https://www.google.com/maps/dir/" + uri + firstLatLong)))));

        } catch (SQLException | URISyntaxException | ScriptingErrorLog.SemanticErrorException ignored) {
        }

    }
}
