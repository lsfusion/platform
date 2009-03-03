package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.util.List;
import java.rmi.RemoteException;
import java.io.IOException;

import net.sf.jasperreports.engine.JRException;

public abstract class ClientNavigator extends AbstractNavigator {

    public RelevantFormNavigator relevantFormNavigator;
    public RelevantClassNavigator relevantClassNavigator;

    public ClientNavigator(RemoteNavigatorInterface iremoteNavigator) {
        super(iremoteNavigator);

        relevantFormNavigator = new RelevantFormNavigator(iremoteNavigator);
        relevantClassNavigator = new RelevantClassNavigator(iremoteNavigator);
    }

    protected List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray(elementID));
    }

    public void currentFormChanged() {
        relevantFormNavigator.tree.createRootNode();
    }

    public void changeCurrentClass(int classID) throws RemoteException {
        if (remoteNavigator.changeCurrentClass(classID))
            relevantClassNavigator.tree.createRootNode();
    }

    public void openRelevantForm(ClientNavigatorForm form) throws IOException, ClassNotFoundException, JRException {
        openForm(form);
    }

    public void openClassForm(ClientNavigatorForm form) throws ClassNotFoundException, IOException, JRException {
        openRelevantForm(form);
    }


    class RelevantFormNavigator extends AbstractNavigator {

        public RelevantFormNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
            openRelevantForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
            return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM : elementID));
        }

    }

    class RelevantClassNavigator extends AbstractNavigator {

        public RelevantClassNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);
        }

        public void openForm(ClientNavigatorForm element) throws ClassNotFoundException, IOException, JRException {
            openClassForm(element);
        }

        protected List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
            return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray((elementID == -1) ? RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS : elementID));
        }

    }

}

