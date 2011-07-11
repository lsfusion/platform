package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import net.sf.jasperreports.engine.JRException;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.ClientModalForm;
import platform.client.ClientResourceBundle;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ViewManager {
    private CControl control;

    List<FormDockable> pages = new ArrayList<FormDockable>();

    private FormFactory pageFactory;

    private CGridArea gridArea;

    private FormRepository forms;

    public ViewManager(CControl control, ClientNavigator mainNavigator) {
        this.control = control;

        pageFactory = new FormFactory(mainNavigator);
        control.addMultipleDockableFactory("page", pageFactory);
        gridArea = control.createWorkingArea("Form area");

        forms = new FormRepository();
        //gridArea.setVisible(true);
    }

    public FormRepository getForms() {
        return forms;
    }

    public CGridArea getGridArea() {
        return gridArea;
    }

    private void openForm(FormDockable page) {
        page.addCDockableStateListener(new CDockableStateAdapter(page));
        page.setLocation(gridArea.getStationLocation());
        control.addDockable(page);
        changeCloseAction(page);
        page.setVisible(true);
        page.comp.requestFocusInWindow();
    }

    public ClientFormDockable openClient(String formSID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = new ClientFormDockable(formSID, navigator, currentSession, pageFactory);
        openForm(page);
        return page;
    }

    public ClientFormDockable openClient(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        ClientFormDockable page = new ClientFormDockable(navigator, remoteForm, pageFactory);
        openForm(page);
        return page;
    }

    public void openModalForm(String formSID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException {
        new ClientModalForm(Main.frame, navigator.remoteNavigator.createForm(formSID, currentSession, true), !currentSession).showDialog();
    }

    public void openReport(String formSID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException {
        openForm(new ReportDockable(formSID, navigator, currentSession, pageFactory));
    }

    public void openReport(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        openForm(new ReportDockable(navigator, remoteForm, pageFactory));
    }

    public void openSingleGroupReport(ClientNavigator navigator, RemoteFormInterface remoteForm, int groupId) throws IOException, ClassNotFoundException {
        openForm(new ReportDockable(navigator, remoteForm, groupId, pageFactory));
    }

    public void openReport(File file) throws JRException {
        openForm(new ReportDockable(file, pageFactory));
    }

    public void changeCloseAction(final FormDockable page) {
        page.setCloseable(false);
        page.setRemoveOnClose(true);
        page.addAction(new CCloseAction(control) {
            @Override
            public void close(CDockable dockable) {
                if (((FormDockable) dockable).pageChanged()) {
                    int n = SwingUtils.showConfirmDialog(
                            page.getComponent(),
                            ClientResourceBundle.getString("form.do.you.really.want.to.close.form"),
                            null,
                            JOptionPane.WARNING_MESSAGE);
                    if (n == JOptionPane.YES_OPTION) {
                        super.close(dockable);
                    }
                } else {
                    super.close(dockable);
                }
            }
        });
    }

    private class FormFactory implements MultipleCDockableFactory<FormDockable, FormLayout> {
        ClientNavigator mainNavigator;

        public FormFactory(ClientNavigator mainNavigator) {
            this.mainNavigator = mainNavigator;
        }

        public FormLayout create() {
            return new FormLayout();
        }

        public FormDockable read(FormLayout layout) {
            try {
                ClientFormDockable page = new ClientFormDockable(layout.getFormSID(), this, mainNavigator);
                page.addCDockableStateListener(new CDockableStateAdapter(page));
                changeCloseAction(page);

                //эмулируем получение фокуса, чтобы не срабатывала переактивация при 1м клике по форме
//                page.getClientForm().getComponent().gainedFocus();
                return page;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public FormLayout write(FormDockable dockable) {
            FormLayout layout = new FormLayout();
            layout.setFormID(dockable.getFormSID());
            return layout;
        }

        public boolean match(FormDockable dockable, FormLayout layout) {
            return false;
        }
    }

    private class CDockableStateAdapter extends CDockableAdapter {
        private FormDockable page;

        public CDockableStateAdapter(FormDockable page) {
            this.page = page;
        }

        @Override
        public void visibilityChanged(CDockable dockable) {
            if (dockable instanceof FormDockable) {
                String sid = ((FormDockable)dockable).getFormSID();
                if (dockable.isVisible()) {
                    pages.add(page);
                    forms.add(sid);
                } else {
                    pages.remove(page);
                    control.remove(page);
                    page.closed();
                    forms.remove(sid);
                }
            }
        }
    }
}
