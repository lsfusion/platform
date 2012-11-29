package platform.fdk.actions.geo;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class GetCoordinatesAddressActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface POIInterface;

    public GetCoordinatesAddressActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{StringClass.get(255)});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        POIInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.getSession();
            DataObject fullAddress = context.getKeyValue(POIInterface);

            final Geocoder geocoder = new Geocoder();
            GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress((String) fullAddress.object).setLanguage("ru").getGeocoderRequest();
            GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);

            if (geocoderResponse!=null && geocoderResponse.getResults().size() != 0) {
                GeocoderResult result = geocoderResponse.getResults().get(0);

                double longitude = result.getGeometry().getLocation().getLng().doubleValue();
                double latitude = result.getGeometry().getLocation().getLat().doubleValue();

                getLCP("readLatitude").change(latitude, session);
                getLCP("readLongitude").change(longitude, session);
            }

        } catch (SQLException e) {
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }
    }
}
