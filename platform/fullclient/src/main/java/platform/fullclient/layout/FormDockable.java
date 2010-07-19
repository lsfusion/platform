package platform.fullclient.layout;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

// уничтожаемые формы
class FormDockable extends DefaultMultipleCDockable {

    final int formID;
    JPanel page;
    Form form;


    public FormDockable(int iformID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        this(iformID, factory, navigator.remoteNavigator.getCaption(iformID));
        createActiveComponent(navigator, navigator.remoteNavigator.createForm(formID, currentSession));
    }

    protected FormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        this(remoteForm.getID(), factory, navigator.remoteNavigator.getCaption(remoteForm.getID()));
        createActiveComponent(navigator, remoteForm);
    }

    FormDockable(int iformID, MultipleCDockableFactory<FormDockable,?> factory, String caption) {
        super(factory);
        formID = iformID;
        setCloseable( true );
        setMinimizable( true );
        setMaximizable( true );
        setExternalizable( false );
        setRemoveOnClose( true );

        page = new JPanel();
        form = new Form(formID, caption);
    }

     FormDockable(int iformID, MultipleCDockableFactory<FormDockable,?> factory, String caption, ClientNavigator navigator) throws RemoteException, ClassNotFoundException, IOException{
        super(factory);
        formID = iformID;
        setTitleText(caption);
        setCloseable( true );
        setMinimizable( true );
        setMaximizable( true );
        setExternalizable( false );
        setRemoveOnClose( true );

        page = new JPanel();
        form = new Form(formID, caption);
        getContentPane().add(new ClientForm(navigator.remoteNavigator.createForm(formID, true), navigator).getComponent());
    }

    void createActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        setActiveComponent(getActiveComponent(navigator, remoteForm), navigator.remoteNavigator.getCaption(formID));
    }

    void setActiveComponent(Component comp, String caption) {
        getContentPane().add(comp);
        setTitleText(caption);
    }

    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        return null; }


    // закрываются пользователем
    void closed(){}

    public Form getForm() {
        return form;
    }
}
