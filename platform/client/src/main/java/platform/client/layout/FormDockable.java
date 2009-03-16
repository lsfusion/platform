package platform.client.layout;

import bibliothek.gui.dock.DefaultDockable;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

import net.sf.jasperreports.engine.JRException;

// уничтожаемые формы
abstract class FormDockable extends DefaultDockable {

    final int formID;

    FormDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        this(iformID);

        createActiveComponent(navigator, currentSession);
    }

    FormDockable(int iformID, ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        this(iformID);

        createActiveComponent(navigator, remoteForm);
    }

    FormDockable(int iformID) {
        formID = iformID;
        setFactoryID(ClientFormFactory.FACTORY_ID);
    }

    void createActiveComponent(ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        createActiveComponent(navigator, navigator.remoteNavigator.createForm(formID, currentSession));
    }

    void createActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        setActiveComponent(getActiveComponent(navigator, remoteForm), navigator.remoteNavigator.getCaption(formID));
    }

    void setActiveComponent(Component comp, String caption) {

        setTitleText(caption);
        add(comp);
    }

    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException { return null; }

//    FormDockable(String caption) {super(caption);}
//    FormDockable(Component Component,String caption) {super(Component,caption);}

    // закрываются пользователем
    abstract void closed();

}
