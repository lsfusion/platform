package lsfusion.client.dock;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CWorkingArea;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CDockableLocationEvent;
import bibliothek.gui.dock.common.event.CDockableLocationListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.form.ClientModalForm;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.JRException;

import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;

public class DockableManager {
    private CControl control;

    private ClientDockableFactory dockableFactory;

    private CWorkingArea formArea;

    private DockableRepository forms;
    
    private ExtendedMode mode = ExtendedMode.NORMALIZED;
    private boolean internalModeChangeOnSetVisible = false;

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

    public CWorkingArea getFormArea() {
        return formArea;
    }

    public ClientDockableFactory getDockableFactory() {
        return dockableFactory;
    }

    public CControl getControl() {
        return control;
    }

    private void openForm(final ClientDockable page) {
        page.addCDockableStateListener(new DockableVisibilityListener());
        page.addCDockableLocationListener(new CDockableLocationListener() {
            @Override
            public void changed(CDockableLocationEvent event) {
                if (event.getOldShowing() != event.getNewShowing()) {
                    page.onShowingChanged(event.getOldShowing(), event.getNewShowing());
                }
            }
        });

        formArea.add(page);

        internalModeChangeOnSetVisible = true;
        page.setVisible(true);
        internalModeChangeOnSetVisible = false;
        
        page.setExtendedMode(mode);
        page.toFront();
        page.requestFocusInWindow();
    }

    public ClientDockable openForm(ClientNavigator navigator, String canonicalName, String formSID) throws IOException, ClassNotFoundException, JRException {
        return openForm(navigator, canonicalName, formSID, 0);
    }

    public ClientDockable openForm(ClientNavigator navigator, String canonicalName, String formSID, int modifiers) throws IOException, ClassNotFoundException, JRException {
        ClientDockable page;
        if (MainFrame.forbidDuplicateForms && forms.getFormsList().contains(formSID) && (modifiers & InputEvent.CTRL_MASK) == 0) { //only when ctrl not pressed
            page = (ClientDockable) control.getCDockable(control.getCDockableCount() - forms.getFormsList().size() + forms.getFormsList().indexOf(formSID));
            if(page != null) {
                page.toFront();
                page.requestFocusInWindow();
            }
        } else {
            page = new ClientFormDockable(navigator, canonicalName, formSID, this);
            openForm(page);
        }
        return page;
    }

    public ClientFormDockable openForm(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, byte[] firstChanges, MainFrame.FormCloseListener closeListener) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = new ClientFormDockable(navigator, canonicalName, formSID, remoteForm, this, closeListener, firstChanges);
        openForm(page);
        return page;
    }

    public void openModalForm(String canonicalName, String formSID, ClientNavigator navigator, boolean showFullScreen) throws IOException, ClassNotFoundException {
        new ClientModalForm(canonicalName, formSID, Main.frame, navigator.remoteNavigator.createForm(formSID, null, true, true)).showDialog(showFullScreen);
    }

    public Integer openReport(ReportGenerationData generationData, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        ClientReportDockable page = new ClientReportDockable(generationData, this, editInvoker);
        openForm(page);
        return page.pageCount;
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
            if (layout.getCanonicalName() == null) {
                return null;
            }
            
            try {
                ClientFormDockable page = mainNavigator.createFormDockableByCanonicalName(layout.getCanonicalName(), DockableManager.this);
                if (page == null) {
                    return null;
                }
                
                page.addCDockableStateListener(new DockableVisibilityListener());

                return page;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public ClientDockableLayout write(ClientDockable dockable) {
            return new ClientDockableLayout(dockable.getCanonicalName());
        }

        public boolean match(ClientDockable dockable, ClientDockableLayout layout) {
            return false;
        }
    }

    private class DockableVisibilityListener extends CDockableAdapter {
        @Override
        public void visibilityChanged(CDockable cdockable) {
            ClientDockable dockable = (ClientDockable) cdockable;

            String canonicalName = dockable.getCanonicalName();
            if (dockable.isVisible()) {
                forms.add(canonicalName);
            } else {
                forms.remove(canonicalName);
                control.removeDockable(dockable);

                dockable.onClosed();
            }
        }

        @Override
        public void maximized(CDockable dockable) {
            if (!internalModeChangeOnSetVisible) {
                DockableManager.this.mode = forms.getFormsList().isEmpty() ? ExtendedMode.NORMALIZED : ExtendedMode.MAXIMIZED;
            }
        }

        @Override
        public void normalized(CDockable dockable) {
            if (!internalModeChangeOnSetVisible) {
                DockableManager.this.mode = ExtendedMode.NORMALIZED;
            }
        }
    }
}
