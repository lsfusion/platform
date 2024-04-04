package lsfusion.server.logics.classes.data.utils.geo;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Iterator;

public class ShowOnMapPathAction extends GeoAction {
    private final ClassPropertyInterface mapProviderInterface;

    public ShowOnMapPathAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        mapProviderInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
        try {

            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);
            boolean isYandex = isYandex(context, mapProvider);

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitude[POI]").getExpr(poiExpr));
            query.addProperty("longitude", findProperty("longitude[POI]").getExpr(poiExpr));
            query.addProperty("numberPathPOI", findProperty("numberPath[POI]").getExpr(context.getModifier(), poiExpr));
            query.addProperty("namePOI", findProperty("name[POI]").getExpr(poiExpr));
            query.and(findProperty("numberPath[POI]").getExpr(context.getModifier(), poiExpr).getWhere());
            ImOrderMap<ImMap<String, Object>, ImMap<Object, Object>> result = query.execute(context, MapFact.singletonOrder("numberPathPOI", false));
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
                context.requestUserInteraction(new OpenUriClientAction((isYandex ?
                        ("https://maps.yandex.ru/?rtt=auto&rtm=atm&rtext=" + uri + firstLatLong) :
                        ("https://www.google.com/maps/dir/" + uri + firstLatLong))));

        } catch (SQLException | ScriptingErrorLog.SemanticErrorException ignored) {
        }

    }
}
