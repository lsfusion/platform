package lsfusion.erp.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jgap.*;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.salesman.Salesman;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CalculatePathActionProperty extends ScriptingActionProperty {

    public CalculatePathActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            KeyExpr poiExpr = new KeyExpr("poi");
            ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("poi", poiExpr);
            QueryBuilder<String, Object> query = new QueryBuilder<>(keys);
            query.addProperty("latitude", findProperty("latitudePOI").getExpr(poiExpr));
            query.addProperty("longitude", findProperty("longitudePOI").getExpr(poiExpr));
            query.addProperty("numberPathPOI", findProperty("numberPathPOI").getExpr(context.getModifier(), poiExpr));
            query.and(findProperty("inPathPOI").getExpr(context.getModifier(), poiExpr).getWhere());
            ImOrderMap<ImMap<String, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context.getSession());
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?";

            String origins = "";
            String destinations = "";
            int index = 1;

            boolean coordinatesFlag = true;
            Object startPathPOI = findProperty("startPathPOI").read(context.getSession().sql, context.getModifier(), context.getQueryEnv());
            if (startPathPOI != null) {
                Map<Integer, DataObject> poiMap = new HashMap<>();

                for (int i = 0, size = result.size(); i < size; i++) {
                    ImMap<Object, ObjectValue> values = result.getValue(i);

                    BigDecimal latitude = (BigDecimal) values.get("latitude").getValue();
                    BigDecimal longitude = (BigDecimal) values.get("longitude").getValue();

                    if (latitude != null && longitude != null) {
                        String latLong = latitude + "," + longitude;
                        origins += (origins.isEmpty() ? "" : "|") + latLong;
                        destinations += (destinations.isEmpty() ? "" : "|") + latLong;
                        DataObject POI = result.getKey(i).singleValue();
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

                        int[][] distances = readDistances(size, origins, destinations, url);

                        TravellingSalesman t = new TravellingSalesman(result.size(), distances);
                        Configuration.reset();
                        IChromosome optimal = t.findOptimalPath(null);
                        for (int i = 0; i < optimal.getGenes().length; i++) {
                            findProperty("numberPathPOI").change(i + 1, context, poiMap.get(optimal.getGene(i).getAllele()));
                        }

                    }
                } else {
                    context.requestUserInteraction(new MessageClientAction("Не все координаты проставлены", "Ошибка"));
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    private int[][] readDistances(int size, String origins, String destinations, String url) throws JSONException, IOException {
        int[][] distances = new int[size][size];
        final JSONObject response = JsonReader.read(url + "origins=" + origins + "&" + "destinations=" + destinations + "&sensor=false");
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
        } else {
            throw new RuntimeException("Reading distances failed: " + response.getString("status"));
        }
        return distances;
    }

    public class TravellingSalesman extends Salesman {
        int cities;
        int[][] distances;
        public TravellingSalesman(int cities, int[][] distances) {
            this.cities = cities;
            this.distances = distances;
        }

        /**
         * Create an array of the given number of
         * integer genes. The first gene is always 0, this is
         * a city where the salesman starts the journey
         */
        public Chromosome createSampleChromosome(Object initial_data) {
            IntegerGene[] genes = new IntegerGene[cities];
            for (int i = 0; i < genes.length; i++) {
                try {
                    genes[i] = new IntegerGene(getConfiguration(), 0, cities - 1);
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                genes[i].setAllele(i);
            }
            Chromosome sample = null;
            try {
                sample = new Chromosome(getConfiguration(), genes);
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
            return sample;
        }

        /**
         * Distance is equal to the difference between city numbers,
         * except the distance between the last and first cities that
         * is equal to 1. In this way, we ensure that the optimal
         * soultion is 0 1 2 3 .. n - easy to check.
         */
        public double distance(Gene a_from, Gene a_to) {
            int a = ((IntegerGene) a_from).intValue();
            int b = ((IntegerGene) a_to).intValue();
            //if (a == 0 && b == cities - 1) return 1;
            //if (b == 0 && a == cities - 1) return 1;
            return distances[a][b];
        }
    }
}