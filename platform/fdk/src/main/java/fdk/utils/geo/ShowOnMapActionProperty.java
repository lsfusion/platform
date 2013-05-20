package fdk.utils.geo;

import platform.interop.action.OpenUriClientAction;
import platform.server.classes.DoubleClass;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;

public class ShowOnMapActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface latitudeInterface;
    private final ClassPropertyInterface longitudeInterface;
    private final ClassPropertyInterface mapProviderInterface;
    private final ClassPropertyInterface addressInterface;

    public ShowOnMapActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{DoubleClass.instance, DoubleClass.instance, LM.findClassByCompoundName("MapProvider"), StringClass.get(100)});

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        latitudeInterface = i.next();
        longitudeInterface = i.next();
        mapProviderInterface = i.next();
        addressInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject latitude = context.getDataKeyValue(latitudeInterface);
            DataObject longitude = context.getDataKeyValue(longitudeInterface);
            DataObject mapProvider = context.getDataKeyValue(mapProviderInterface);
            DataObject addressMap = context.getDataKeyValue(addressInterface);

            if (latitude.object != null && longitude.object != null) {
                String url =  ((String)getLCP("staticName").read(context, mapProvider)).contains("yandex")  ?
                        ("http://maps.yandex.ru/?"+ "text=" + addressMap.object.toString().trim().replace(" ","%20").replace(",", "%2C") + "&ll=" + longitude.object + "+%2C" + latitude.object + "&z=17") :
                        ("http://maps.google.com/?q=loc:" + latitude.object + "+" + longitude.object);

                context.requestUserInteraction(new OpenUriClientAction(new URI(url)));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}
