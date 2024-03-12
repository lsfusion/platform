package lsfusion.server.logics.classes.data.utils.geo;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DistanceGeoAction extends GeoAction {
    int partSize = 25; //google restriction: max 25 origins and 25 destinations

    public DistanceGeoAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public DistanceGeoAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
    }

    protected int[] readDistances(int size, String origins, String destinations, int attempt) throws JSONException, IOException, InterruptedException {
        int[] distances = new int[size];
        if(!destinations.isEmpty()) {
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?" + "origins=" + origins + "&" + "destinations=" + destinations + "&sensor=false";
            final JSONObject response = JSONReader.read(url);
            if (response.getString("status").equals("OK")) {
                JSONArray locations = response.getJSONArray("rows");
                JSONArray locationList = locations.getJSONObject(0).getJSONArray("elements");
                for (int j = 0; j < locationList.length(); j++) {
                    JSONObject location = locationList.getJSONObject(j);
                    if (location.get("status").equals("OK"))
                        distances[j] = (Integer) location.getJSONObject("distance").get("value");
                }
            } else {
                if(attempt >= 3) {
                    throw new RuntimeException("Reading distances failed: " + response.getString("status"));
                } else {
                    Thread.sleep(10000);
                    return readDistances(size, origins, destinations, attempt + 1);
                }
            }
        }
        return distances;
    }

}
