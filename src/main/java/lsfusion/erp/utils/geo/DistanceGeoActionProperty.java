package lsfusion.erp.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.silvertunnel_ng.netlib.api.util.TcpipNetAddress;
import org.silvertunnel_ng.netlib.util.HttpUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class DistanceGeoActionProperty extends GeoActionProperty {
    NetLayer netLayer = null;
    int partSize = 25; //google restriction: max 25 origins and 25 destinations

    public DistanceGeoActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public DistanceGeoActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected int[] readDistances(int size, String origins, String destinations, boolean useTor, int attempt) throws JSONException, IOException, InterruptedException {
        int[] distances = new int[size];
        if(!destinations.isEmpty()) {
            if(netLayer == null && useTor)
                netLayer = getNetLayer();
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?" + "origins=" + origins + "&" + "destinations=" + destinations + "&sensor=false";
            final JSONObject response = useTor ? getDocumentTor(netLayer, url) : JsonReader.read(url);
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
                    return readDistances(size, origins, destinations, useTor, attempt + 1);
                }
            }
        }
        return distances;
    }

    private NetLayer getNetLayer() throws IOException {
        NetLayer lowerNetLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR);
        // wait until TOR is ready (optional):
        lowerNetLayer.waitUntilReady();
        return lowerNetLayer;
    }

    private JSONObject getDocumentTor(NetLayer lowerNetLayer, String url) throws IOException, JSONException {
        int count = 2;
        while (count > 0) {
            try {
                Thread.sleep(50);

                // prepare parameters
                TcpipNetAddress httpServerNetAddress = new TcpipNetAddress("maps.googleapis.com", 80);
                long timeoutInMs = 10000;

                // do the request and wait for the response
                byte[] responseBody = new HttpUtil().get(lowerNetLayer, httpServerNetAddress, url, timeoutInMs);
                return new JSONObject(Jsoup.parse(new ByteArrayInputStream(responseBody), "utf-8", "").text());
            } catch (HttpStatusException e) {
                count--;
                if(count <= 0)
                    ServerLoggers.systemLogger.error("DistanceGeo Error: ", e);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }
        return new JSONObject("");
    }
}
