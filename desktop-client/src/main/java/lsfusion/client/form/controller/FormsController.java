package lsfusion.client.form.controller;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CWorkingArea;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CDockableLocationEvent;
import bibliothek.gui.dock.common.event.CDockableLocationListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.facile.lookandfeel.DockableCollector;
import bibliothek.gui.dock.support.lookandfeel.LookAndFeelUtilities;
import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.base.view.ColorThemeChangeListener;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.print.view.ClientReportDockable;
import lsfusion.client.form.print.view.EditReportInvoker;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.form.view.ClientModalForm;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.sf.jasperreports.engine.JRException;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FormsController implements ColorThemeChangeListener {
    private CControl control;

    private ClientDockableFactory dockableFactory;

    private CWorkingArea formArea;

    private DockableRepository forms;
    
    private ExtendedMode mode = ExtendedMode.NORMALIZED;
    private boolean internalModeChangeOnSetVisible = false;

    public List<ClientDockable> openedForms = new ArrayList<>();

    private ClientDockable prevFocusPage = null;
    DockableCollector dockableCollector;

    public FormsController(CControl control, ClientNavigator mainNavigator) {
        this.control = control;
        this.dockableFactory = new ClientDockableFactory(mainNavigator);
        this.formArea = control.createWorkingArea("Form area");
        this.forms = new DockableRepository();

        dockableCollector = new DockableCollector(control.intern());

        control.addMultipleDockableFactory("page", dockableFactory);
        MainController.addColorThemeChangeListener(this);

        //Global KeyEvents listener
        KeyboardFocusManager fm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        fm.addKeyEventPostProcessor(e -> {
            if(e.getID() == KeyEvent.KEY_PRESSED && !e.isConsumed()) {
                Window activeWindow = fm.getActiveWindow();
                if(activeWindow instanceof DockableMainFrame) {
                    CDockable page = control.getFocusedCDockable();
                    if(page instanceof ClientFormDockable) {
                        ((ClientFormDockable) page).directProcessKeyEvent(e);
                    }
                } else if(activeWindow instanceof ClientModalForm) {
                    ((ClientModalForm) activeWindow).directProcessKeyEvent(e);
                }
            }
            return false;
        });
    }
    
    public void clean() {
        // close forms to remove objects not cleaned by GC on restart
        for (ClientDockable openedForm : new ArrayList<>(openedForms)) {
            openedForm.setVisible(false);
        }
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
                    if(event.getOldShowing()) {
                        prevFocusPage = page;
                    }
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

        page.onOpened();
        openedForms.add(page);
    }

    public ClientFormDockable openForm(ClientNavigator navigator, String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, byte[] firstChanges, MainFrame.FormCloseListener closeListener) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = null;
        if (MainController.forbidDuplicateForms && forbidDuplicate && forms.getFormsList().contains(formSID)) {
            ClientDockable dockable = (ClientDockable) control.getCDockable(control.getCDockableCount() - forms.getFormsList().size() + forms.getFormsList().indexOf(formSID));
            if (dockable instanceof ClientFormDockable)
                page = (ClientFormDockable) dockable;
        }

        if(page != null) {
            page.toFront();
            page.requestFocusInWindow();
        } else {
            page = new ClientFormDockable(navigator, canonicalName, formSID, remoteForm, this, closeListener, firstChanges);
            openForm(page);
        }
        page.addAction(new CloseAllAction(openedForms));
        return page;
    }

    public Integer openReport(ReportGenerationData generationData, String formCaption, String printerName, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        ClientReportDockable page = new ClientReportDockable(generationData, this, formCaption, printerName, editInvoker);
        openForm(page);
        return page.pageCount;
    }

    @Override
    public void colorThemeChanged() {
        LookAndFeelUtilities.updateUI(dockableCollector.listComponents());
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
            return null;
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
                openedForms.remove(dockable);
                //return focus to previous page (by default in maximized mode after closing page focus returns to the last page from the list)
                if(prevFocusPage != null) {
                    ExtendedMode mode = prevFocusPage.getExtendedMode();
                    if(mode != null && mode == ExtendedMode.MAXIMIZED) {
                        prevFocusPage.toFront();
                        prevFocusPage.requestFocusInWindow();
                    }
                }
            }
        }

        @Override
        public void maximized(CDockable dockable) {
            if (!internalModeChangeOnSetVisible) {
                FormsController.this.mode = forms.getFormsList().isEmpty() ? ExtendedMode.NORMALIZED : ExtendedMode.MAXIMIZED;
            }
        }

        @Override
        public void normalized(CDockable dockable) {
            if (!internalModeChangeOnSetVisible) {
                FormsController.this.mode = ExtendedMode.NORMALIZED;
            }
        }
    }
}
