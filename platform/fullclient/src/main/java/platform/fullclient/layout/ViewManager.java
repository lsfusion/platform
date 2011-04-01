package platform.fullclient.layout;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGridArea;
import bibliothek.gui.dock.common.MultipleCDockableFactory;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import net.sf.jasperreports.engine.JRException;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ViewManager {
    private CControl control;

    List<FormDockable> pages = new ArrayList<FormDockable>();

    private FormFactory pageFactory;

    private CGridArea gridArea;

    public ViewManager(CControl control, ClientNavigator mainNavigator) {
        this.control = control;

        pageFactory = new FormFactory(mainNavigator);
        control.addMultipleDockableFactory("page", pageFactory);
        gridArea = control.createWorkingArea("Form area");
        gridArea.setVisible(true);
    }

    public CGridArea getGridArea() {
        return gridArea;
    }

    private void openForm(FormDockable page) {
        page.addCDockableStateListener(new CDockableStateAdapter(page));
        page.setLocation(gridArea.getStationLocation());
        control.add(page);
        changeCloseAction(page);
        page.setVisible(true);
        page.comp.requestFocusInWindow();
    }

    public void openClient(String formSID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        openForm(new ClientFormDockable(formSID, navigator, currentSession, pageFactory));
    }

    public void openClient(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        openForm(new ClientFormDockable(navigator, remoteForm, pageFactory));
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

    public void openReport(String fileName, String directory) throws JRException {
        openForm(new ReportDockable(fileName, directory, pageFactory));
    }

    public void changeCloseAction(FormDockable page) {
        page.setCloseable(false);
        page.setRemoveOnClose(true);
        page.addAction(new CCloseAction(control) {
            @Override
            public void close(CDockable dockable) {
                if (((FormDockable) dockable).pageChanged()) {
                    int n = JOptionPane.showConfirmDialog(
                            null,
                            "Вы действительно хотите закрыть окно, не применив изменения в базу данных?",
                            "LS Fusion",
                            JOptionPane.YES_NO_OPTION);
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
            if (dockable.isVisible()) {
                pages.add(page);
            } else {
                pages.remove(page);
                control.remove(page);
                page.closed();
            }
        }
    }
}
