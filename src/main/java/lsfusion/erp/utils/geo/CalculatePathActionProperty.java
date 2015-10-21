package lsfusion.erp.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
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
import java.util.*;

public class CalculatePathActionProperty extends DistanceGeoActionProperty {


    public CalculatePathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            boolean useTor = findProperty("useTor").read(context) != null;

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitudePOI").getExpr(poiExpr));
            query.addProperty("longitude", findProperty("longitudePOI").getExpr(poiExpr));
            query.addProperty("numberPathPOI", findProperty("numberPathPOI").getExpr(context.getModifier(), poiExpr));
            query.and(findProperty("inPathPOI").getExpr(context.getModifier(), poiExpr).getWhere());
            ImOrderMap<ImMap<String, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());

            boolean coordinatesFlag = true;
            Object startPathPOI = findProperty("startPathPOI").read(context.getSession().sql, context.getModifier(), context.getQueryEnv());
            if (startPathPOI != null) {
                Map<Integer, DataObject> poiMap = new HashMap<>();
                Map<Integer, String> points = new HashMap<>();
                int index = 1;

                for (int i = 0, size = result.size(); i < size; i++) {
                    ImMap<Object, ObjectValue> values = result.getValue(i);

                    BigDecimal latitude = (BigDecimal) values.get("latitude").getValue();
                    BigDecimal longitude = (BigDecimal) values.get("longitude").getValue();

                    if (latitude != null && longitude != null) {
                        String latLong = latitude + "," + longitude;

                        DataObject POI = result.getKey(i).singleValue();
                        if (POI.getValue().equals(startPathPOI)) {
                            points.put(0, latLong);
                            poiMap.put(0, POI);
                        }
                        else {
                            points.put(index, latLong);
                            poiMap.put(index, POI);
                            index++;
                        }
                    } else {
                        coordinatesFlag = false;
                        break;
                    }
                }

                if (coordinatesFlag) {
                    int size = result.values().size();
                    if (size != 0) {

                        Map<Pair<DataObject, DataObject>, Integer> distanceMap = getDistancesMap(context);

                        int[][] distances = new int[size][size];
                        for (int i = 0; i < size; i++) {
                            try (DataSession session = context.createSession()) {
                                int[] localDistances = new int[size];
                                List<Integer> queryIndices = new ArrayList<>();
                                String destinations = "";
                                int count = 0;
                                for (int j = 0; j < size; j++) {
                                    if (i != j) {
                                        Integer localDistance = distanceMap.get(Pair.create(poiMap.get(i), poiMap.get(j)));
                                        if (localDistance == null) {
                                            destinations += (destinations.isEmpty() ? "" : "|") + points.get(j);
                                            queryIndices.add(j);
                                            count++;
                                            if (count % partSize == 0) {
                                                ServerLoggers.systemLogger.info(String.format("Getting distance between point %s and %s others", i + 1, partSize));
                                                int[] partDistances = readDistances(partSize, points.get(i), destinations, useTor, 0);
                                                for (int k = 0; k < partDistances.length; k++) {
                                                    localDistances[queryIndices.get(k)] = partDistances[k]; 
                                                }
                                                destinations = "";
                                                queryIndices.clear();
                                            }
                                        }
                                    }
                                }
                                if (!destinations.isEmpty()) {
                                    ServerLoggers.systemLogger.info(String.format("Getting distance between point %s and %s, others", i + 1, count % partSize));
                                    int[] partDistances = readDistances(count % partSize, points.get(i), destinations, useTor, 0);
                                    for (int k = 0; k < partDistances.length; k++) {
                                        localDistances[queryIndices.get(k)] = partDistances[k];
                                    }
                                }
                                for (int j = 0; j < size; j++) {
                                    if (i != j) {
                                        Integer distance = distanceMap.get(Pair.create(poiMap.get(i), poiMap.get(j)));
                                        if (distance == null) {
                                            distance = localDistances[j];
                                            findProperty("distancePOIPOI").change(distance, session, poiMap.get(i), poiMap.get(j));
                                        }
                                        distances[i][j] = distance;
                                    }
                                }
                                session.apply(context);
                            }
                        }

                        //Hamiltonian
                        HamiltonianCycleHelper h = new HamiltonianCycleHelper(distances);
                        int[] output = h.execute();
                        
                        for (int i = 0; i < output.length; i++) {
                            findProperty("numberPathPOI").change(i + 1, context, poiMap.get(output[i]));
                        }

                    }
                } else {
                    context.requestUserInteraction(new MessageClientAction("Не все координаты проставлены", "Ошибка"));
                }
            } else {
                context.requestUserInteraction(new MessageClientAction("Не задана начальная точка", "Ошибка"));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    private Map<Pair<DataObject, DataObject>, Integer> getDistancesMap(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr poi1Expr = new KeyExpr("poi");
        KeyExpr poi2Expr = new KeyExpr("poi");
        ImRevMap<String, KeyExpr> keys = MapFact.toRevMap("poi1", poi1Expr, "poi2", poi2Expr);
        QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
        query.addProperty("distancePOIPOI", findProperty("distancePOIPOI").getExpr(poi1Expr, poi2Expr));
        query.and(findProperty("inPathPOI").getExpr(context.getModifier(), poi1Expr, poi2Expr).getWhere());
        query.and(findProperty("distancePOIPOI").getExpr(poi1Expr, poi2Expr).getWhere());
        ImOrderMap<ImMap<String, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());

        Map<Pair<DataObject, DataObject>, Integer> distanceMap = new HashMap<>();
        for (int i = 0, size = result.size(); i < size; i++) {
            ImMap<String, DataObject> keysMap = result.getKey(i);
            ImMap<Object, ObjectValue> valuesMap = result.getValue(i);

            Integer distance = (Integer) valuesMap.get("distancePOIPOI").getValue();
            distanceMap.put(Pair.create(keysMap.get("poi1"), keysMap.get("poi2")), distance);
        }
        return distanceMap;
    }
}