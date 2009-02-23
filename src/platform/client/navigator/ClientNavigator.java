package platform.client.navigator;

import platform.server.view.navigator.RemoteNavigator;
import platform.interop.ByteArraySerializer;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.navigator.AbstractNavigator;

import java.util.List;

public abstract class ClientNavigator extends AbstractNavigator {

    public RelevantFormNavigator relevantFormNavigator;
    public RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigator iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    protected List<ClientNavigatorElement> getNodeElements(int elementID) {
        return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray(elementID));
    }

    public void currentFormChanged() {
        relevantFormNavigator.tree.createRootNode();
    }

    public void changeCurrentClass(int classID) {
        if (remoteNavigator.changeCurrentClass(classID))
            relevantClassNavigator.tree.createRootNode();
    }

    public void openRelevantForm(ClientNavigatorForm form) {
        openForm(form);
    }

    public void openClassForm(ClientNavigatorForm form) {
        openRelevantForm(form);
    }


    class RelevantFormNavigator extends AbstractNavigator {

        public RelevantFormNavigator(RemoteNavigator iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            openRelevantForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTFORM : elementID));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigator iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            openClassForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return ByteArraySerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigator.NAVIGATORGROUP_RELEVANTCLASS : elementID));
        }

    }

}

