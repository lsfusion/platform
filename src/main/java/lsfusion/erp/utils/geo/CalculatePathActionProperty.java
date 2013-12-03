package lsfusion.erp.utils.geo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class CalculatePathActionProperty extends ScriptingActionProperty {

    public CalculatePathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            LCP<PropertyInterface> isPOI = (LCP<PropertyInterface>) LM.is(LM.getClassByName("POI"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isPOI.getMapKeys();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("latitude", LM.getLCPByOldName("latitudePOI").getExpr(keys.singleValue()));
            query.addProperty("longitude", LM.getLCPByOldName("longitudePOI").getExpr(keys.singleValue()));
            query.addProperty("numberPathPOI", LM.getLCPByOldName("numberPathPOI").getExpr(context.getModifier(), keys.singleValue()));
            query.and(isPOI.property.getExpr(keys).getWhere());
            query.and(LM.getLCPByOldName("inPathPOI").getExpr(context.getModifier(), keys.singleValue()).getWhere());
            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?";

            String origins = "origins=";
            String destinations = "destinations=";
            int index = 1;

            boolean coordinatesFlag = true;
            Object startPathPOI = getLCP("startPathPOI").read(context.getSession().sql, context.getModifier(), context.getQueryEnv());
            if (startPathPOI != null) {
                Map<Integer, DataObject> poiMap = new HashMap<Integer, DataObject>();
                Map<DataObject, String> poiLatLongMap = new HashMap<DataObject, String>();
                for (int i = 0, size = result.size(); i < size; i++) {
                    ImMap<Object, ObjectValue> values = result.getValue(i);

                    BigDecimal latitude = (BigDecimal) values.get("latitude").getValue();
                    BigDecimal longitude = (BigDecimal) values.get("longitude").getValue();

                    if (latitude != null && longitude != null) {
                        String prefix = index != 1 ? "|" : "";
                        String latLong = latitude + "," + longitude;
                        origins += prefix + latLong;
                        destinations += prefix + latLong;
                        DataObject POI = result.getKey(i).singleValue();
                        poiLatLongMap.put(POI, latLong);
                        if (POI.getValue().equals(startPathPOI))
                            poiMap.put(0, POI);
                        else {
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
                            Map<String, Integer> addressMap = new HashMap<String, Integer>();
                            List<Object[]> orders = new ArrayList<Object[]>();
                            for (int element : path) {
                                String latLong = poiLatLongMap.get(poiMap.get(element));
                                DataObject POI = poiMap.get(element);
                                int i = addressMap.containsKey(latLong) ? addressMap.get(latLong) : (element + 1);
                                orders.add(new Object[]{i, POI});
                                addressMap.put(latLong, i);
                            }

                            Collections.sort(orders, new OrdersComparator());

                            int offset = 0;
                            int previous = 0;
                            for (Object[] entry : orders) {
                                Integer order = (Integer) entry[0];
                                DataObject POI = (DataObject) entry[1];
                                if ((order - previous) > 1)
                                    offset += order - previous - 1;
                                previous = order;
                                getLCP("numberPathPOI").change(order - offset, context.getSession(), POI);
                            }
                        }
                    }
                } else {
                    context.requestUserInteraction(new MessageClientAction("Не все координаты проставлены", "Ошибка"));
                }
            }
        } catch (SQLException e) {
        } catch (JSONException e) {
        } catch (IOException e) {
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }

    }

    class OrdersComparator implements Comparator {
        public int compare(Object ord1, Object ord2) {
            int ord1Key = (Integer) ((Object[]) ord1)[0];
            int ord2Key = (Integer) ((Object[]) ord2)[0];
            if (ord1Key > ord2Key)
                return 1;
            else if (ord1Key < ord2Key)
                return -1;
            else
                return 0;
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
