package lsfusion.server.logics.classes.data.utils.geo;

import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
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

public class DistanceGeoAction extends GeoAction {
    NetLayer netLayer = null;
    int partSize = 25; //google restriction: max 25 origins and 25 destinations

    public DistanceGeoAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public DistanceGeoAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
    }

    protected int[] readDistances(int size, String origins, String destinations, boolean useTor, int attempt) throws JSONException, IOException, InterruptedException {
        int[] distances = new int[size];
        if(!destinations.isEmpty()) {
            if(netLayer == null && useTor)
                netLayer = getNetLayer();
            String url = "http://maps.googleapis.com/maps/api/distancematrix/json?" + "origins=" + origins + "&" + "destinations=" + destinations + "&sensor=false";
            final JSONObject response = useTor ? getDocumentTor(netLayer, url) : JSONReader.read(url);
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

    private NetLayer getNetLayer() {
        NetLayer lowerNetLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR);
        // wait until TOR is ready (optional):
        lowerNetLayer.waitUntilReady();
        return lowerNetLayer;
    }

    private JSONObject getDocumentTor(NetLayer lowerNetLayer, String url) throws IOException, JSONException {
        int count = 2;
        while (count > 0) {
            try {
                ThreadUtils.sleep(50);

                // prepare parameters
                TcpipNetAddress httpServerNetAddress = new TcpipNetAddress("maps.googleapis.com", 80);
                long timeoutInMs = 10000;

                // do the request and wait for the response
                byte[] responseBody = new HttpUtil().get(lowerNetLayer, httpServerNetAddress, url, timeoutInMs);
                return new JSONObject(Jsoup.parse(new ByteArrayInputStream(responseBody), "utf-8", "").text());
            } catch (HttpStatusException | JSONException e) {
                count--;
                if(count <= 0)
                    ServerLoggers.systemLogger.error("DistanceGeo Error, url: " + url, e);
            }
        }
        return new JSONObject("");
    }
}
