package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.util.List;

import static platform.client.navigator.ClientNavigatorElement.BASE_ELEMENT_SID;

public class RelevantFormNavigatorPanel extends AbstractNavigatorPanel {
    public RelevantFormNavigatorPanel(ClientNavigator clientNavigator) {
        super(clientNavigator);
    }

    @Override
    protected List<ClientNavigatorElement> getNodeElements(ClientNavigatorElement element) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                clientNavigator.remoteNavigator.getElementsByteArray(
                        element.getSID().equals(BASE_ELEMENT_SID)
                        ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM
                        : element.getSID()
                ), clientNavigator.windows);
    }

    public void currentFormChanged() {
        tree.createRootNode();
    }
}
