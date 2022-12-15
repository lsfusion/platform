package lsfusion.gwt.server.convert;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.RawFileData;
import lsfusion.client.navigator.ClientCaptionElementNavigator;
import lsfusion.client.navigator.ClientImageElementNavigator;
import lsfusion.client.navigator.ClientNavigatorChanges;
import lsfusion.client.navigator.ClientPropertyNavigator;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.navigator.GCaptionElementNavigator;
import lsfusion.gwt.client.navigator.GImageElementNavigator;
import lsfusion.gwt.client.navigator.GPropertyNavigator;
import lsfusion.gwt.server.FileUtils;
import lsfusion.interop.logics.ServerSettings;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
public class ClientNavigatorChangesToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientNavigatorChangesToGwtConverter instance = new ClientNavigatorChangesToGwtConverter();
    }

    public static ClientNavigatorChangesToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientNavigatorChangesToGwtConverter() {
    }

    @Cached
    @Converter(from = ClientNavigatorChanges.class)
    public GNavigatorChangesDTO convertNavigatorChanges(ClientNavigatorChanges clientChanges, ServletContext servletContext, ServerSettings settings) {
        GNavigatorChangesDTO navigatorChanges = new GNavigatorChangesDTO();
        navigatorChanges.properties = new GPropertyNavigator[clientChanges.properties.size()];
        navigatorChanges.values = new Serializable[clientChanges.properties.size()];
        int i = 0;
        for (Map.Entry<ClientPropertyNavigator, Object> entry : clientChanges.properties.entrySet()) {
            navigatorChanges.properties[i] = convertOrCast(entry.getKey());

            Serializable value;
            if(entry.getValue() instanceof AppImage) { //static image
                value = FileUtils.createImageFile(servletContext, settings, (AppImage) entry.getValue(), false);
            } else if (entry.getValue() instanceof RawFileData) { //dynamic image
                value = FileUtils.saveApplicationFile((RawFileData) entry.getValue());
            } else {
                value = (Serializable) entry.getValue();
            }

            navigatorChanges.values[i] = value;
            i++;
        }
        return navigatorChanges;
    }

    @Cached
    @Converter(from = ClientCaptionElementNavigator.class)
    public GCaptionElementNavigator convertCaptionElementNavigator(ClientCaptionElementNavigator captionElementNavigator) {
        return new GCaptionElementNavigator(captionElementNavigator.canonicalName);
    }

    @Cached
    @Converter(from = ClientImageElementNavigator.class)
    public GImageElementNavigator convertImageElementNavigator(ClientImageElementNavigator imageElementNavigator) {
        return new GImageElementNavigator(imageElementNavigator.canonicalName);
    }
}