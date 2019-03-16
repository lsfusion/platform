package lsfusion.server.logics.classes.data.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.builder.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.logging.ServerLoggers;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RecalculateDistancePOIAction extends DistanceGeoAction {
    private final ClassPropertyInterface POIInterface;

    public RecalculateDistancePOIAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        POIInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            DataObject poiObject = context.getDataKeyValue(POIInterface);
            boolean useTor = findProperty("useTor[]").read(context) != null;

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitude[POI]").getExpr(context.getModifier(), poiExpr));
            query.addProperty("longitude", findProperty("longitude[POI]").getExpr(context.getModifier(), poiExpr));
            query.and(findProperty("distancePOIPOI[POI,POI]").getExpr(poiExpr, poiObject.getExpr()).getWhere().or(
                    findProperty("distancePOIPOI[POI,POI]").getExpr(poiObject.getExpr(), poiExpr).getWhere()));
            query.and(findProperty("latitude[POI]").getExpr(context.getModifier(), poiExpr).getWhere());
            query.and(findProperty("longitude[POI]").getExpr(context.getModifier(), poiExpr).getWhere());
            ImOrderMap<ImMap<String, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());

            Map<Integer, DataObject> poiMap = new HashMap<>();
            Map<Integer, String> points = new HashMap<>();

            for (int i = 0, size = result.size(); i < size; i++) {
                ImMap<Object, ObjectValue> values = result.getValue(i);

                BigDecimal latitude = (BigDecimal) values.get("latitude").getValue();
                BigDecimal longitude = (BigDecimal) values.get("longitude").getValue();
                String latLong = latitude + "," + longitude;
                DataObject POI = result.getKey(i).singleValue();
                points.put(i, latLong);
                poiMap.put(i, POI);
            }
            int size = points.size();
            if (size > 0) {

                BigDecimal latitude = (BigDecimal) findProperty("latitude[POI]").read(context, poiObject);
                BigDecimal longitude = (BigDecimal) findProperty("longitude[POI]").read(context, poiObject);
                String latLong = latitude + "," + longitude;


                try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                    String destinations = "";
                    int count = 0;
                    int[] localDistances = new int[size];
                    for (int i = 0; i < size; i++) {
                        destinations += (destinations.isEmpty() ? "" : "|") + points.get(i);
                        count++;
                        if(count % partSize == 0) {
                            ServerLoggers.systemLogger.info(String.format("Getting distance between point %s and %s others", latLong, partSize));
                            int[] partDistances = readDistances(partSize, latLong, destinations, useTor, 0);
                            System.arraycopy(partDistances, 0, localDistances, count - partSize, partDistances.length);
                            destinations = "";
                        }
                    }
                    if(!destinations.isEmpty()) {
                        ServerLoggers.systemLogger.info(String.format("Getting distance between point %s and %s others", latLong, count % partSize));
                        int[] partDistances = readDistances(count % partSize, latLong, destinations, useTor, 0);
                        System.arraycopy(partDistances, 0, localDistances, (int) Math.floor((double) count / partSize) * partSize, partDistances.length);
                    }
                    for (int i = 0; i < points.size(); i++) {
                        if (points.containsKey(i)) {
                            findProperty("distancePOIPOI[POI,POI]").change(localDistances[i], newContext, poiObject, poiMap.get(i));
                            findProperty("distancePOIPOI[POI,POI]").change(localDistances[i], newContext, poiMap.get(i), poiObject);
                        }
                    }
                    newContext.apply();
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}