package lsfusion.server.logics.classes.data.utils.geo;

import com.google.common.base.Throwables;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;

public class GetCoordinatesAddressAction extends GeoAction {
    private final ClassPropertyInterface addressInterface;
    private final ClassPropertyInterface mapProviderInterface;

    public GetCoordinatesAddressAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        addressInterface = i.next();
        mapProviderInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
        try {
            DataSession session = context.getSession();
            DataObject fullAddress = context.getDataKeyValue(addressInterface);
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);

            BigDecimal longitude;
            BigDecimal latitude;
            String address = (String) fullAddress.object;
            if (address != null) {
                String apiKey = (String) findProperty("apiKey[]").read(context);
                if (isYandex(context, mapProvider)) {
                    String addressParam = address.trim().replace(" ", "+");
                    String url = "https://geocode-maps.yandex.ru/1.x/?geocode=" + URLEncoder.encode(addressParam, String.valueOf(StandardCharsets.UTF_8)) + "&apikey=" + apiKey + "&results=1&format=json";

                    final JSONObject response = JSONReader.read(url);
                    JSONObject objectCollection = response.getJSONObject("response").getJSONObject("GeoObjectCollection");
                    JSONObject featureMember = (JSONObject) objectCollection.getJSONArray("featureMember").get(0);
                    JSONObject point = featureMember.getJSONObject("GeoObject").getJSONObject("Point");
                    String[] position = point.getString("pos").split(" ");

                    longitude = new BigDecimal(position[0]);
                    latitude = new BigDecimal(position[1]);
                } else {
                    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
                    GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
                    LatLng location = results[0].geometry.location;

                    longitude = BigDecimal.valueOf(location.lng);
                    latitude = BigDecimal.valueOf(location.lat);
                }

                findProperty("readLatitude[]").change(latitude, session);
                findProperty("readLongitude[]").change(longitude, session);
            }
        } catch (IOException | JSONException | SQLException | ScriptingErrorLog.SemanticErrorException | InterruptedException | ApiException e) {
            throw Throwables.propagate(e);
        }
    }
}
