package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

public abstract class ClientNavigator extends AbstractNavigator {

    public final RelevantFormNavigator relevantFormNavigator;
    public final RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigatorInterface iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    public void currentFormChanged() {
        relevantFormNavigator.tree.createRootNode();
    }

    private int classID = 0; // временная оптимизация

    public void changeCurrentClass(int classID) throws RemoteException {
        if (classID != 0 && this.classID != classID) {
            this.classID = classID;
            relevantClassNavigator.tree.createRootNode();
        }
    }

    public void openRelevantForm(ClientNavigatorForm form) throws IOException, ClassNotFoundException {
        openForm(form);
    }

    void openClassForm(ClientNavigatorForm form) throws ClassNotFoundException, IOException {
        openRelevantForm(form);
    }

    public void openModalForm(ClientNavigatorForm form) throws ClassNotFoundException, IOException {
        openRelevantForm(form);
    }

    class RelevantFormNavigator extends AbstractNavigator {

        public RelevantFormNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
            openRelevantForm(element);
        }

        @Override
        protected List<ClientNavigatorElement> getNodeElements(ClientNavigatorElement element) throws IOException {
            return DeSerializer.deserializeListClientNavigatorElement(
                    remoteNavigator.getElementsByteArray(element.getSID().equals(AbstractNavigator.BASE_ELEMENT_SID) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM : element.getSID()));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
            openClassForm(element);
        }

        @Override
        protected List<ClientNavigatorElement> getNodeElements(ClientNavigatorElement element) throws IOException {
            return DeSerializer.deserializeListClientNavigatorElement(
                    remoteNavigator.getElementsByteArray(element.getSID().equals(BASE_ELEMENT_SID) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS : element.getSID()));
        }

    }

}