package platform.fdk.actions.geo;

import platform.interop.action.OpenUriClientAction;
import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class ShowOnMapActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface latitudeInterface;
    private final ClassPropertyInterface longitudeInterface;

    public ShowOnMapActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{DoubleClass.instance, DoubleClass.instance});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        latitudeInterface = i.next();
        longitudeInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject latitude = context.getKeyValue(latitudeInterface);
            DataObject longitude = context.getKeyValue(longitudeInterface);

            if (latitude.object != null && longitude.object != null)
                context.requestUserInteraction(new OpenUriClientAction(new URI("http://maps.google.com/?q=loc:" + latitude.object + "+" + longitude.object)));
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
