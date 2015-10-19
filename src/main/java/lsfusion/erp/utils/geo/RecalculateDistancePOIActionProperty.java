package lsfusion.erp.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RecalculateDistancePOIActionProperty extends DistanceGeoActionProperty {
    private final ClassPropertyInterface POIInterface;

    public RecalculateDistancePOIActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        POIInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            DataObject poiObject = context.getDataKeyValue(POIInterface);
            boolean useTor = findProperty("useTor").read(context) != null;

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitudePOI").getExpr(poiExpr));
            query.addProperty("longitude", findProperty("longitudePOI").getExpr(poiExpr));
            query.and(findProperty("distancePOIPOI").getExpr(poiExpr, poiObject.getExpr()).getWhere().or(
                    findProperty("distancePOIPOI").getExpr(poiObject.getExpr(), poiExpr).getWhere()));
            query.and(findProperty("latitudePOI").getExpr(poiExpr).getWhere());
            query.and(findProperty("longitudePOI").getExpr(poiExpr).getWhere());
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

                BigDecimal latitude = (BigDecimal) findProperty("latitudePOI").read(context, poiObject);
                BigDecimal longitude = (BigDecimal) findProperty("longitudePOI").read(context, poiObject);
                String latLong = latitude + "," + longitude;


                try (DataSession session = context.createSession()) {
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
                            findProperty("distancePOIPOI").change(localDistances[i], session, poiObject, poiMap.get(i));
                            findProperty("distancePOIPOI").change(localDistances[i], session, poiMap.get(i), poiObject);
                        }
                    }
                    session.apply(context);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}