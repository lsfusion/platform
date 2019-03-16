package lsfusion.server.logics.classes.data.utils.geo;

import com.google.common.base.Throwables;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;

public class ShowOnMapAction extends GeoAction {
    private final ClassPropertyInterface latitudeInterface;
    private final ClassPropertyInterface longitudeInterface;
    private final ClassPropertyInterface mapProviderInterface;
    private final ClassPropertyInterface addressInterface;

    public ShowOnMapAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        latitudeInterface = i.next();
        longitudeInterface = i.next();
        mapProviderInterface = i.next();
        addressInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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
