package lsfusion.erp.utils.geo;

import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.DoubleClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
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
