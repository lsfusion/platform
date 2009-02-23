package platform.client.layout;

import bibliothek.gui.dock.DefaultDockable;
import platform.client.navigator.ClientNavigator;
import platform.server.view.form.RemoteForm;

import java.sql.SQLException;
import java.awt.*;

// уничтожаемые формы
abstract class FormDockable extends DefaultDockable {

    int formID;

    FormDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws SQLException {
        this(iformID);

        createActiveComponent(navigator, currentSession);
    }

    FormDockable(int iformID, ClientNavigator navigator, RemoteForm remoteForm) throws SQLException {
        this(iformID);

        createActiveComponent(navigator, remoteForm);
    }

    FormDockable(int iformID) {
        formID = iformID;
        setFactoryID(ClientFormFactory.FACTORY_ID);
    }

    void createActiveComponent(ClientNavigator navigator, boolean currentSession) throws SQLException {
        createActiveComponent(navigator, navigator.remoteNavigator.createForm(formID, currentSession));
    }

    void createActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {
        setActiveComponent(getActiveComponent(navigator, remoteForm), navigator.remoteNavigator.getCaption(formID));
    }

    void setActiveComponent(Component comp, String caption) {

        setTitleText(caption);
        add(comp);
    }

    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) { return null; }

//    FormDockable(String caption) {super(caption);}
//    FormDockable(Component Component,String caption) {super(Component,caption);}

    // закрываются пользователем
    abstract void closed();

}
