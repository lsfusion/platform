package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.util.List;

public abstract class ClientNavigator extends AbstractNavigator {

    public RelevantFormNavigator relevantFormNavigator;
    public RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigatorInterface iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    protected List<ClientNavigatorElement> getNodeElements(int elementID) {
        return DeSerializer.deserializeListClientNavigatorElement(
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

        public RelevantFormNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            openRelevantForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM : elementID));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) {
            openClassForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) {
            return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS : elementID));
        }

    }

}

