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
import java.sql.SQLException;
import java.util.Iterator;

public class GetAddressCoordinatesAction extends GeoAction {
    private final ClassPropertyInterface latitudeInterface;
    private final ClassPropertyInterface longitudeInterface;
    private final ClassPropertyInterface mapProviderInterface;

    public GetAddressCoordinatesAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        latitudeInterface = i.next();
        longitudeInterface = i.next();
        mapProviderInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
        try {
            DataSession session = context.getSession();
            BigDecimal latitude = (BigDecimal) context.getDataKeyValue(latitudeInterface).object;
            BigDecimal longitude = (BigDecimal) context.getDataKeyValue(longitudeInterface).object;
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);

            String address;
            if (latitude != null && longitude != null) {
                String apiKey = (String) findProperty("apiKey[MapProvider]").read(context, mapProvider);
                if (isYandex(context, mapProvider)) {
                    String url = "https://geocode-maps.yandex.ru/1.x/?apikey=" + apiKey + "&geocode=" + longitude + "," + latitude + "&results=1&format=json";

                    JSONObject featureMember = (JSONObject) JSONReader.read(url).getJSONObject("response").getJSONObject("GeoObjectCollection")
                            .getJSONArray("featureMember").get(0);
                    address = featureMember.getJSONObject("GeoObject").getJSONObject("metaDataProperty")
                            .getJSONObject("GeocoderMetaData").getJSONObject("Address").getString("formatted");

                } else {
                    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
                    GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, new LatLng(latitude.doubleValue(), longitude.doubleValue())).await();
                    address = results[0].formattedAddress;
                }

                findProperty("readAddress[]").change(address, session);
            }
        } catch (IOException | JSONException | SQLException | ScriptingErrorLog.SemanticErrorException | InterruptedException | ApiException e) {
            throw Throwables.propagate(e);
        }
    }
}
