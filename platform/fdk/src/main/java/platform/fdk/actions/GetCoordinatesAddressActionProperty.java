package platform.fdk.actions;

import geo.google.GeoAddressStandardizer;
import geo.google.GeoException;
import geo.google.datamodel.GeoAddress;
import geo.google.datamodel.GeoCoordinate;
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

            GeoAddressStandardizer st = new GeoAddressStandardizer("apikey");
            List<GeoAddress> addresses = st.standardizeToGeoAddresses(((String)fullAddress.object).trim());
            GeoAddress address = addresses.get(0);
            GeoCoordinate coords = address.getCoordinate();
            double longitude = coords.getLongitude();
            double latitude = coords.getLatitude();

            getLCP("readLatitude").change(latitude, session);
            getLCP("readLongitude").change(longitude, session);

        } catch (GeoException e) {
        } catch (SQLException e) {
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }
    }
}
