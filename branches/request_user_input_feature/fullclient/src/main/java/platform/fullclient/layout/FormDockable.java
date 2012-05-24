package platform.fullclient.layout;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;

// уничтожаемые формы
abstract class FormDockable extends DefaultMultipleCDockable {

    private String formSID;
    Component comp;

    protected FormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory) {
        super(factory);
        this.formSID = formSID;
        setMinimizable(true);
        setMaximizable(true);
        setExternalizable(false);
        setRemoveOnClose(true);
    }

    protected FormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator, RemoteFormInterface remoteForm, FormUserPreferences userPreferences) throws ClassNotFoundException, IOException {
        this(formSID, factory);
        createActiveComponent(navigator, remoteForm, userPreferences);
    }

    protected FormDockable(String formSID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable, ?> factory, boolean interactive, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException {
        this(formSID, factory, navigator, navigator.remoteNavigator.createForm(formSID, null, currentSession, interactive), userPreferences);
    }

    protected FormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable, ?> factory, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException {
        this(remoteForm.getSID(), factory, navigator, remoteForm, userPreferences);
    }

    protected FormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator, FormUserPreferences userPreferences) throws ClassNotFoundException, IOException {
        this(formSID, navigator, true, factory, true, userPreferences);
    }

    private void createActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException {
        setActiveComponent(getActiveComponent(navigator, remoteForm, userPreferences), getCaption());
    }

    void setActiveComponent(Component comp, String caption) {
        getContentPane().add(comp);
        this.comp = comp;
        setTitleText(caption);
        /*
        addFocusListener(new CFocusListener() {
            public void focusGained(CDockable dockable) {
                ((FormDockable) dockable).comp.requestFocusInWindow();
            }

            public void focusLost(CDockable dockable) {
            }
        });
        */
    }

    // закрываются пользователем
    void closed() {
        getContentPane().removeAll();
    }

    abstract Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException;

    protected abstract String getCaption();

    public String getFormSID() {
        return formSID;
    }

    public boolean pageChanged() {
        return false;
    }

    public Component getComponent() {
        return comp;
    }
}
