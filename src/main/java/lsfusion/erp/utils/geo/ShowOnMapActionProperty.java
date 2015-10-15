package lsfusion.erp.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;

public class ShowOnMapActionProperty extends GeoActionProperty {
    private final ClassPropertyInterface latitudeInterface;
    private final ClassPropertyInterface longitudeInterface;
    private final ClassPropertyInterface mapProviderInterface;
    private final ClassPropertyInterface addressInterface;

    public ShowOnMapActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        latitudeInterface = i.next();
        longitudeInterface = i.next();
        mapProviderInterface = i.next();
        addressInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject latitude = context.getDataKeyValue(latitudeInterface);
            DataObject longitude = context.getDataKeyValue(longitudeInterface);
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);
            DataObject addressMap = context.getDataKeyValue(addressInterface);

            if (latitude.object != null && longitude.object != null) {
                String url =  isYandex(context, mapProvider)  ?
                        ("http://maps.yandex.ru/?"+ "text=" + addressMap.object.toString().trim().replace(" ","%20").replace(",", "%2C").replace("\"", "") + "&ll=" + longitude.object + "+%2C" + latitude.object + "&z=17") :
                        ("http://maps.google.com/?q=loc:" + latitude.object + "+" + longitude.object);

                context.requestUserInteraction(new OpenUriClientAction(new URI(url)));
            }
        } catch (URISyntaxException | SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
