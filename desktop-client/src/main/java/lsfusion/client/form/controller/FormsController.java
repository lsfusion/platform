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
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.print.view.ClientReportDockable;
import lsfusion.client.form.print.view.EditReportInvoker;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.form.view.ClientModalForm;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.ClientNavigatorAction;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.controller.AsyncFormController;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.FormClientData;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public class FormsController implements ColorThemeChangeListener {
    private CControl control;

    private ClientDockableFactory dockableFactory;

    private CWorkingArea formArea;

    private DockableRepository forms;
    
    private ExtendedMode mode = ExtendedMode.NORMALIZED;
    private boolean internalModeChangeOnSetVisible = false;

    private Long lastCompletedRequest = -1L;
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
                    processBinding(new KeyInputEvent(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx())), e);
                } else if(activeWindow instanceof ClientModalForm) {
                    ((ClientModalForm) activeWindow).directProcessKeyEvent(e);
                }
            }
            return false;
        });
    }

    private final Map<InputEvent, List<ClientFormController.Binding>> bindings = new HashMap<>();
    private final List<ClientFormController.Binding> keySetBindings = new ArrayList<>();

    public void addBindings(ClientNavigatorElement element, InputEvent inputEvent, Integer priority) {
        if (inputEvent != null) {
            bindings.put(inputEvent, Collections.singletonList(new ClientFormController.Binding(null, nvl(priority, 0)) {
                @Override
                public boolean pressed(java.awt.event.InputEvent ke) {
                    if (element instanceof ClientNavigatorAction) {
                        KeyboardFocusManager fm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                        Window activeWindow = fm.getActiveWindow();
                        if (activeWindow instanceof DockableMainFrame) {
                            DockableMainFrame f = (DockableMainFrame) activeWindow;
                            f.executeNavigatorAction((ClientNavigatorAction) element, (ke.getModifiers() & java.awt.event.InputEvent.CTRL_MASK) != 0, true);
                        }
                    }
                    return true;
                }

                @Override
                public boolean showing() {
                    return true;
                }
            }));
        }
    }

    public void processBinding(InputEvent ks, java.awt.event.InputEvent ke) {
        ProcessBinding.processBinding(ks, false, ke, () -> null, false, (groupObject, binding) -> true,
                bindings, keySetBindings, (binding, preview) -> true, binding -> true, (groupObject, binding) -> true,
                (binding, event) -> true, binding -> true, (binding, panel) -> true, () -> {});
    }

    public boolean isDialog() {
        return false;
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
                    } else if (page instanceof ClientFormDockable) {
                        ((ClientFormDockable) page).focusGained();
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

        ((DockableMainFrame) MainFrame.instance).setLogsDockableVisible(false);
    }

    public ClientFormDockable openForm(AsyncFormController asyncFormController, ClientNavigator navigator, boolean forbidDuplicate,
                                       RemoteFormInterface remoteForm, FormClientData clientData, MainFrame.FormCloseListener closeListener, String formId) throws IOException {
        ClientForm clientForm = ClientFormController.deserializeClientForm(remoteForm, clientData);
        ClientFormDockable page = asyncFormController.removeAsyncForm();
        boolean asyncOpened = page != null;

        if (!asyncOpened) {
            ClientFormDockable duplicateForm = getDuplicateForm(clientData.canonicalName, forbidDuplicate);
            if (duplicateForm != null) {
                duplicateForm.toFront();
                duplicateForm.requestFocusInWindow();
                return duplicateForm;
            }
        }

        if (!asyncOpened) {
            if (openFormTimer != null) {
                openFormTimer.stop();
                openFormTimer = null;
            }
            page = new ClientFormDockable(clientForm.canonicalName, clientForm.getCaption(), this, openedForms, null, false);
        } else {
            page.getContentPane().removeAll(); //remove loading
        }
        page.init(navigator, remoteForm, clientForm, closeListener, clientData, formId);
        if (!asyncOpened) {
            openForm(page);
        }
        return page;
    }

    //we don't want flashing, so we use timer
    Timer openFormTimer;
    public void asyncOpenForm(AsyncFormController asyncFormController, ClientAsyncOpenForm asyncOpenForm) {
        if (getDuplicateForm(asyncOpenForm.canonicalName, asyncOpenForm.forbidDuplicate) == null) {
            openFormTimer = new Timer(100, e -> {
                if(openFormTimer != null) {
                    if (asyncFormController.checkNotCompleted()) { //request is not completed yet
                        ClientFormDockable page = new ClientFormDockable(asyncOpenForm.canonicalName, asyncOpenForm.caption, FormsController.this, openedForms, asyncFormController, true);
                        page.asyncInit();
                        openForm(page);
                        asyncFormController.putAsyncForm(page);
                    }
                    openFormTimer = null;
                }
            });
            openFormTimer.setRepeats(false);
            openFormTimer.start();
        }
    }

    private ClientFormDockable getDuplicateForm(String canonicalName, boolean forbidDuplicate) {
        if (MainController.forbidDuplicateForms && forbidDuplicate) {
            List<ClientDockable> formsList = forms.getFormsList();
            ClientDockable duplicate = formsList.stream().filter(dockable -> dockable.getCanonicalName() != null && dockable.getCanonicalName().equals(canonicalName)).findFirst().orElse(null);
            if (duplicate != null) {
                CDockable dockable = control.getCDockable(control.getCDockableCount() - formsList.size() + formsList.indexOf(duplicate));
                if (dockable instanceof ClientFormDockable) {
                    return (ClientFormDockable) dockable;
                }
            }
        }
        return null;
    }

    public void setLastCompletedRequest(Long lastCompletedRequest) {
        this.lastCompletedRequest = lastCompletedRequest;
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

            if (dockable.isVisible()) {
                forms.add(dockable);
            } else {
                forms.remove(dockable);
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
