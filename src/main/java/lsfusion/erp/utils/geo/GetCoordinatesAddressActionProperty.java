package lsfusion.erp.utils.geo;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import lsfusion.server.data.SQLHandledException;
import org.json.JSONException;
import org.json.JSONObject;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Iterator;

public class GetCoordinatesAddressActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface POIInterface;
    private final ClassPropertyInterface mapProviderInterface;

    public GetCoordinatesAddressActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        POIInterface = i.next();
        mapProviderInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataSession session = context.getSession();
            DataObject fullAddress = context.getDataKeyValue(POIInterface);
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);

            BigDecimal longitude = null;
            BigDecimal latitude = null;
            String address = (String) fullAddress.object;
            if (address != null) {

                if (((String) findProperty("staticName").read(session, mapProvider)).contains("yandex")) {

                    String url = "http://geocode-maps.yandex.ru/1.x/?geocode=" + address.trim().replace(" ", "+") + "&results=1&format=json";

                    final JSONObject response = JsonReader.read(url);
                    if (response != null) {
                        JSONObject objectCollection = response.getJSONObject("response").getJSONObject("GeoObjectCollection");
                        JSONObject featureMember = (JSONObject) objectCollection.getJSONArray("featureMember").get(0);
                        JSONObject point = featureMember.getJSONObject("GeoObject").getJSONObject("Point");
                        String position[] = point.getString("pos").split(" ");

                        longitude = new BigDecimal(position[0]);
                        latitude = new BigDecimal(position[1]);
                    }
                } else {

                    final Geocoder geocoder = new Geocoder();
                    GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(address).setLanguage("ru").getGeocoderRequest();
                    GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);

                    if (geocoderResponse != null && geocoderResponse.getResults().size() != 0) {
                        GeocoderResult result = geocoderResponse.getResults().get(0);

                        longitude = result.getGeometry().getLocation().getLng();
                        latitude = result.getGeometry().getLocation().getLat();
                    }
                }

                findProperty("readLatitude").change(latitude, session);
                findProperty("readLongitude").change(longitude, session);
            }
        } catch (MalformedURLException ignored) {
        } catch (IOException ignored) {
        } catch (JSONException ignored) {
        } catch (SQLException ignored) {
        } catch (ScriptingErrorLog.SemanticErrorException ignored) {
        }
    }
}
