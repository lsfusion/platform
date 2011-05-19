package platform.fullclient.layout;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

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

    protected FormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        this(formSID, factory);
        createActiveComponent(navigator, remoteForm);
    }

    protected FormDockable(String formSID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable, ?> factory, boolean interactive) throws IOException, ClassNotFoundException {
        this(formSID, factory, navigator, navigator.remoteNavigator.createForm(formSID, currentSession, interactive));
    }

    protected FormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable, ?> factory) throws IOException, ClassNotFoundException {
        this(remoteForm.getSID(), factory, navigator, remoteForm);
    }

    protected FormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator) throws ClassNotFoundException, IOException {
        this(formSID, factory, navigator, navigator.remoteNavigator.createForm(formSID, true, true));
    }

    private void createActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        setActiveComponent(getActiveComponent(navigator, remoteForm), getCaption());
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

    abstract Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException;

    protected abstract String getCaption();

    public String getFormSID() {
        return formSID;
    }

    public boolean pageChanged() {
        return false;
    }
}
