package lsfusion.gwt.server.convert;

import lsfusion.client.navigator.*;
import lsfusion.client.navigator.window.ClientClassWindowNavigator;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.navigator.*;
import lsfusion.gwt.client.navigator.window.GClassWindowNavigator;
import lsfusion.gwt.server.MainDispatchServlet;

import java.io.IOException;
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
    public GNavigatorChangesDTO convertNavigatorChanges(ClientNavigatorChanges clientChanges, MainDispatchServlet servlet, String sessionID) throws IOException {
        GNavigatorChangesDTO navigatorChanges = new GNavigatorChangesDTO();
        navigatorChanges.properties = new GPropertyNavigator[clientChanges.properties.size()];
        navigatorChanges.values = new Serializable[clientChanges.properties.size()];
        int i = 0;
        for (Map.Entry<ClientPropertyNavigator, Object> entry : clientChanges.properties.entrySet()) {
            navigatorChanges.properties[i] = convertOrCast(entry.getKey());
            navigatorChanges.values[i] = ClientFormChangesToGwtConverter.convertFileValue(entry.getValue(), null, servlet, sessionID);
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

    @Cached
    @Converter(from = ClientClassElementNavigator.class)
    public GClassElementNavigator convertClassElementNavigator(ClientClassElementNavigator imageElementNavigator) {
        return new GClassElementNavigator(imageElementNavigator.canonicalName);
    }

    @Cached
    @Converter(from = ClientClassWindowNavigator.class)
    public GClassWindowNavigator convertClassWindowNavigator(ClientClassWindowNavigator imageElementNavigator) {
        return new GClassWindowNavigator(imageElementNavigator.canonicalName);
    }

    @Cached
    @Converter(from = ClientShowIfElementNavigator.class)
    public GShowIfElementNavigator convertShowIfElementNavigator(ClientShowIfElementNavigator showIfElementNavigator) {
        return new GShowIfElementNavigator(showIfElementNavigator.canonicalName);
    }
}