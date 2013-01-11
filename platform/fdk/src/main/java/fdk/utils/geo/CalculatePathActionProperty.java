package fdk.utils.geo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CalculatePathActionProperty extends ScriptingActionProperty {

    public CalculatePathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
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
            query.and(isPOI.property.getExpr(keys).getWhere());
            query.and(LM.getLCPByName("inPathPOI").getExpr(context.getModifier(), keys.singleValue()).getWhere());
            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?";

            String origins = "origins=";
            String destinations = "destinations=";
            int index = 1;

            Object startPathPOI = getLCP("startPathPOI").read(context.getSession().sql, context.getModifier(), context.getQueryEnv());
            if (startPathPOI != null) {
                Map<Integer, DataObject> poiMap = new HashMap<Integer, DataObject>();
                for (int i=0,size=result.size();i<size;i++) {
                    ImMap<Object, ObjectValue> values = result.getValue(i);

                    Double latitude = (Double) values.get("latitude").getValue();
                    Double longitude = (Double) values.get("longitude").getValue();

                    if (latitude != null && longitude != null) {
                        String prefix = index != 1 ? "|" : "";
                        origins += prefix + latitude + "," + longitude;
                        destinations += prefix + latitude + "," + longitude;
                        DataObject POI = result.getKey(i).singleValue();
                        if (POI.getValue().equals(startPathPOI))
                            poiMap.put(0, POI);
                        else {
                            poiMap.put(index, POI);
                            index++;
                        }
                    }
                }
                int size = result.values().size();
                if (size != 0) {

                    int[][] distances = new int[size][size];

                    final JSONObject response = JsonReader.read(url + origins + "&" + destinations + "&sensor=false");
                    if (response.getString("status").equals("OK")) {
                        JSONArray locations = response.getJSONArray("rows");

                        for (int i = 0; i < locations.length(); i++) {
                            JSONArray locationList = locations.getJSONObject(i).getJSONArray("elements");
                            for (int j = 0; j < locationList.length(); j++) {
                                JSONObject location = locationList.getJSONObject(j);
                                if (location.get("status").equals("OK"))
                                    distances[i][j] = (Integer) location.getJSONObject("distance").get("value");
                            }
                        }

                        int[] path = getShortestPath(distances);

                        for (int element : path) {
                            getLCP("numberPathPOI").change(element+1, context.getSession(), poiMap.get(element));
                        }
                    }
                }
            }
        } catch (SQLException e) {
        } catch (JSONException e) {
        } catch (IOException e) {
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }

    }

    public static int[] getShortestPath(int[][] dist) {
        int n = dist.length;
        int[][] dp = new int[1 << n][n];
        for (int[] d : dp)
            Arrays.fill(d, Integer.MAX_VALUE / 2);
        dp[1][0] = 0;
        for (int mask = 1; mask < 1 << n; mask += 2) {
            for (int i = 1; i < n; i++) {
                if ((mask & 1 << i) != 0) {
                    for (int j = 0; j < n; j++) {
                        if ((mask & 1 << j) != 0) {
                            dp[mask][i] = Math.min(dp[mask][i], dp[mask ^ (1 << i)][j] + dist[j][i]);
                        }
                    }
                }
            }
        }
        int res = Integer.MAX_VALUE;
        for (int i = 1; i < n; i++) {
            res = Math.min(res, dp[(1 << n) - 1][i] + dist[i][0]);
        }

        // reconstruct path
        int cur = (1 << n) - 1;
        int[] order = new int[n];
        int last = 0;
        for (int i = n - 1; i >= 1; i--) {
            int bj = -1;
            for (int j = 1; j < n; j++) {
                if ((cur & 1 << j) != 0 && (bj == -1 || dp[cur][bj] + dist[bj][last] > dp[cur][j] + dist[j][last])) {
                    bj = j;
                }
            }
            order[i] = bj;
            cur ^= 1 << bj;
            last = bj;
        }
        return order;
    }
}
