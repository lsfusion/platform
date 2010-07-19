package platform.fullclient.layout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.util.xml.XElement;
import net.sf.jasperreports.engine.JRException;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;


public class ViewManager {
    private CControl control;
    private FormRepository forms;

    List<FormDockable> pages = new ArrayList<FormDockable>();

    private FormFactory pageFactory;


    private CGridArea gridArea;

    public ViewManager(CControl control, ClientNavigator mainNavigator) {
        this.control = control;
        forms = new FormRepository();

        pageFactory = new FormFactory(mainNavigator);
        control.addMultipleDockableFactory("page", pageFactory);
        gridArea = control.createGridArea("Form area");
        gridArea.setVisible(true);
    }

    public FormRepository getForms() {
        return forms;
    }

    public CGridArea getGridArea() {
        return gridArea;
    }


    public void openClient(int iformID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        try {
            final ClientFormDockable page = new ClientFormDockable(iformID, navigator, currentSession, pageFactory);
            forms.add(page.getForm());
            page.addCDockableStateListener(new CDockableAdapter() {
                @Override
                public void visibilityChanged(CDockable dockable) {
                    if (dockable.isVisible()) {
                        pages.add(page);
                    } else {
                        pages.remove(page);
                    }
                }
            });

            page.setLocation(gridArea.getStationLocation());
            control.add(page);
            page.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openClient(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        final ClientFormDockable page = new ClientFormDockable(navigator, remoteForm, pageFactory);
        forms.add(page.getForm());
        page.addCDockableStateListener(new CDockableAdapter() {
            @Override
            public void visibilityChanged(CDockable dockable) {
                if (dockable.isVisible()) {
                    pages.add(page);
                } else {
                    pages.remove(page);
                }
            }
        });
        control.add(page);
        page.setLocation(gridArea.getStationLocation());
        page.setVisible(true);
    }

    public void openReport(int iformID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException {
        final ReportDockable page = new ReportDockable(iformID, navigator, currentSession, pageFactory);
        forms.add(page.getForm());
        page.addCDockableStateListener(new CDockableAdapter() {
            @Override
            public void visibilityChanged(CDockable dockable) {
                if (dockable.isVisible()) {
                    pages.add(page);
                } else {
                    pages.remove(page);
                }
            }
        });

        control.add(page);
        page.setLocation(gridArea.getStationLocation());
        page.setVisible(true);
    }

    public void openReport(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        final ReportDockable page = new ReportDockable(navigator, remoteForm, pageFactory);
        forms.add(page.getForm());
        page.addCDockableStateListener(new CDockableAdapter() {
            @Override
            public void visibilityChanged(CDockable dockable) {
                if (dockable.isVisible()) {
                    pages.add(page);
                } else {
                    pages.remove(page);
                }
            }
        });
        control.add(page);
        page.setLocation(gridArea.getStationLocation());
        page.setVisible(true);
    }

    public void openReport(String fileName,String directory) throws JRException {

        final ReportDockable page = new ReportDockable(fileName, directory, pageFactory);
        forms.add(page.getForm());
        page.addCDockableStateListener(new CDockableAdapter() {
            @Override
            public void visibilityChanged(CDockable dockable) {
                if (dockable.isVisible()) {
                    pages.add(page);
                } else {
                    pages.remove(page);
                }
            }
        });
        control.add(page);
        page.setLocation(gridArea.getStationLocation());
        page.setVisible(true);
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
                String name = layout.getName();

                Form picture = forms.getPicture(name);
                if (picture == null)
                    return null;

                final FormDockable page = new FormDockable(picture.getType(), this, picture.getName(), mainNavigator);

                page.addCDockableStateListener(new CDockableAdapter() {
                    @Override
                    public void visibilityChanged(CDockable dockable) {
                        if (dockable.isVisible()) {
                            pages.add(page);
                        } else {
                            pages.remove(page);
                        }
                    }
                });
                return page;
            }
            catch (Exception e) {
                throw new RuntimeException("Ошибка при чтении схемы", e);
            }
        }         

        public FormLayout write(FormDockable dockable) {
            FormLayout layout = new FormLayout();
            layout.setName(dockable.getForm().getName());
            return layout;
        }       
    }


    private static class FormLayout implements MultipleCDockableLayout {
        /**
         * the name of the picture
         */
        private String name;

        /**
         * Sets the name of the picture that is shown.
         *
         * @param name the name of the picture
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the name of the picture that is shown.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        public void readStream(DataInputStream in) throws IOException {
            name = in.readUTF();
        }

        public void readXML(XElement element) {
            name = element.getString();
        }

        public void writeStream(DataOutputStream out) throws IOException {
            out.writeUTF(name);
        }

        public void writeXML(XElement element) {
            element.setString(name);
        }
    }
}
