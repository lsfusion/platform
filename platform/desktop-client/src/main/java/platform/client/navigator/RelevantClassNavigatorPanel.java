package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.util.List;

import static platform.client.navigator.ClientNavigatorElement.BASE_ELEMENT_SID;

public class RelevantClassNavigatorPanel extends AbstractNavigatorPanel {
    private int currentClass = 0;

    public RelevantClassNavigatorPanel(ClientNavigator clientNavigator) {
        super(clientNavigator);
    }

    @Override
    protected List<ClientNavigatorElement> getNodeElements(ClientNavigatorElement element) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                clientNavigator.remoteNavigator.getElementsByteArray(
                        element.getSID().equals(BASE_ELEMENT_SID)
                        ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS
                        : element.getSID()
                ), clientNavigator.windows);
    }

    public void updateCurrentClass(int classID) {
        if (classID != 0 && this.currentClass != classID) {
            this.currentClass = classID;
            tree.createRootNode();
        }
    }
}
