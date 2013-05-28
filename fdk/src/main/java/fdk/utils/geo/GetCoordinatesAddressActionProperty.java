package fdk.utils.geo;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import org.json.JSONException;
import org.json.JSONObject;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Iterator;

public class GetCoordinatesAddressActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface POIInterface;
    private final ClassPropertyInterface mapProviderInterface;

    public GetCoordinatesAddressActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{StringClass.get(255), LM.findClassByCompoundName("MapProvider")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        POIInterface = i.next();
        mapProviderInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.getSession();
            DataObject fullAddress = context.getDataKeyValue(POIInterface);
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);

            BigDecimal longitude = null;
            BigDecimal latitude = null;
            String address = (String) fullAddress.object;
            if (address != null) {

                if (((String)getLCP("staticName").read(session, mapProvider)).contains("yandex")) {

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

                getLCP("readLatitude").change(latitude, session);
                getLCP("readLongitude").change(longitude, session);
            }
        } catch (MalformedURLException ignored) {
        } catch (IOException ignored) {
        } catch (JSONException ignored) {
        } catch (SQLException ignored) {
        } catch (ScriptingErrorLog.SemanticErrorException ignored) {
        }
    }
}
