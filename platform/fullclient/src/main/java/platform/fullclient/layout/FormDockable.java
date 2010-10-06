package platform.fullclient.layout;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.intern.CDockable;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;

// уничтожаемые формы
abstract class FormDockable extends DefaultMultipleCDockable {

    final int formID;
    Component comp;

    protected FormDockable(int formID, MultipleCDockableFactory<FormDockable, ?> factory) {
        super(factory);
        this.formID = formID;
        setMinimizable(true);
        setMaximizable(true);
        setExternalizable(false);
        setRemoveOnClose(true);
    }

    protected FormDockable(int formID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        this(formID, factory);
        createActiveComponent(navigator, remoteForm);
    }

    protected FormDockable(int formID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable, ?> factory) throws IOException, ClassNotFoundException {
        this(formID, factory, navigator, navigator.remoteNavigator.createForm(formID, currentSession));
    }

    protected FormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable, ?> factory) throws IOException, ClassNotFoundException {
        this(remoteForm.getID(), factory, navigator, remoteForm);
    }

    protected FormDockable(int formID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator) throws RemoteException, ClassNotFoundException, IOException {
        this(formID, factory, navigator, navigator.remoteNavigator.createForm(formID, true));
    }

    private void createActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        setActiveComponent(getActiveComponent(navigator, remoteForm), getCaption());
    }

    void setActiveComponent(Component comp, String caption) {
        getContentPane().add(comp);
        this.comp = comp;
        setTitleText(caption);

        addFocusListener(new CFocusListener() {
            public void focusGained(CDockable dockable) {
                
                ((FormDockable) dockable).comp.requestFocusInWindow();
            }

            public void focusLost(CDockable dockable) {
            }
        });
    }

    // закрываются пользователем
    void closed() {
        getContentPane().removeAll();
    }

    abstract Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException;

    protected abstract String getCaption();

    public int getFormID() {
        return formID;
    }

    public boolean pageChanged() {
        return false;
    }
}
