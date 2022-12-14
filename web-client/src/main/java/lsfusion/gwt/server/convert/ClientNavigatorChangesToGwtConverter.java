package lsfusion.gwt.server.convert;

import lsfusion.client.navigator.ClientCaptionElementNavigator;
import lsfusion.client.navigator.ClientNavigatorChanges;
import lsfusion.client.navigator.ClientPropertyNavigator;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.navigator.GCaptionElementNavigator;
import lsfusion.gwt.client.navigator.GPropertyNavigator;

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
    public GNavigatorChangesDTO convertNavigatorChanges(ClientNavigatorChanges clientChanges) {
        GNavigatorChangesDTO navigatorChanges = new GNavigatorChangesDTO();
        navigatorChanges.properties = new GPropertyNavigator[clientChanges.properties.size()];
        navigatorChanges.values = new Serializable[clientChanges.properties.size()];
        int i = 0;
        for (Map.Entry<ClientPropertyNavigator, Object> entry : clientChanges.properties.entrySet()) {
            navigatorChanges.properties[i] = convertOrCast(entry.getKey());
            navigatorChanges.values[i] = (Serializable) entry.getValue();
            i++;
        }
        return navigatorChanges;
    }

    @Cached
    @Converter(from = ClientCaptionElementNavigator.class)
    public GPropertyNavigator convertPropertyNavigator(ClientCaptionElementNavigator clientPropertyNavigator) {
        return new GCaptionElementNavigator(clientPropertyNavigator.canonicalName);
    }
}