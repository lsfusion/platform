package lsfusion.client.dock;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGridArea;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.form.ClientModalForm;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.JRException;

import java.io.File;
import java.io.IOException;

public class DockableManager {
    private CControl control;

    private ClientDockableFactory dockableFactory;

    private CGridArea formArea;

    private DockableRepository forms;

    public DockableManager(CControl control, ClientNavigator mainNavigator) {
        this.control = control;
        this.dockableFactory = new ClientDockableFactory(mainNavigator);
        this.formArea = control.createWorkingArea("Form area");
        this.forms = new DockableRepository();

        control.addMultipleDockableFactory("page", dockableFactory);
    }

    public DockableRepository getForms() {
        return forms;
    }

    public CGridArea getFormArea() {
        return formArea;
    }

    public ClientDockableFactory getDockableFactory() {
        return dockableFactory;
    }

    public CControl getControl() {
        return control;
    }

    private void openForm(ClientDockable page) {
        page.addCDockableStateListener(new DockableVisibilityListener());
        page.setLocation(formArea.getStationLocation());
        control.addDockable(page);
        page.setVisible(true);
        page.toFront();
        page.requestFocusInWindow();
    }

    public ClientFormDockable openForm(ClientNavigator navigator, String formSID) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = new ClientFormDockable(navigator, formSID, this);
        openForm(page);
        return page;
    }

    public ClientFormDockable openForm(ClientNavigator navigator, RemoteFormInterface remoteForm, MainFrame.FormCloseListener closeListener) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = new ClientFormDockable(navigator, remoteForm, this, closeListener);
        openForm(page);
        return page;
    }

    public void openModalForm(String formSID, ClientNavigator navigator, boolean showFullScreen) throws IOException, ClassNotFoundException {
        new ClientModalForm(Main.frame, navigator.remoteNavigator.createForm(formSID, null, true, true)).showDialog(showFullScreen);
    }

    public void openReport(String reportSID, ReportGenerationData generationData, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        openForm(new ClientReportDockable(reportSID, generationData, this, editInvoker));
    }

    public void openReport(File file) throws JRException {
        openForm(new ClientReportDockable(file, this));
    }

    private class ClientDockableFactory implements MultipleCDockableFactory<ClientDockable, ClientDockableLayout> {
        ClientNavigator mainNavigator;

        public ClientDockableFactory(ClientNavigator mainNavigator) {
            this.mainNavigator = mainNavigator;
        }

        public ClientDockableLayout create() {
            return new ClientDockableLayout();
        }

        public ClientDockable read(ClientDockableLayout layout) {
            try {
                ClientFormDockable page = new ClientFormDockable(mainNavigator, layout.getFormSID(), DockableManager.this);
                page.addCDockableStateListener(new DockableVisibilityListener());

                return page;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public ClientDockableLayout write(ClientDockable dockable) {
            return new ClientDockableLayout(dockable.getFormSID());
        }

        public boolean match(ClientDockable dockable, ClientDockableLayout layout) {
            return false;
        }
    }

    private class DockableVisibilityListener extends CDockableAdapter {
        @Override
        public void visibilityChanged(CDockable cdockable) {
            ClientDockable dockable = (ClientDockable) cdockable;

            String sid = dockable.getFormSID();
            if (dockable.isVisible()) {
                forms.add(sid);
            } else {
                forms.remove(sid);
                control.removeDockable(dockable) ;

                dockable.onClosed();
            }
        }
    }
}
